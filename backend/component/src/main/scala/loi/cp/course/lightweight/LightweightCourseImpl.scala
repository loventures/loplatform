/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package loi.cp.course
package lightweight

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Rpc}
import com.learningobjects.cpxp.component.site.ItemSiteComponent
import com.learningobjects.cpxp.component.web.{ErrorResponse, HtmlResponse, RedirectResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentService, UserException}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.attachment.ImageFacade
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.user.{UserDTO, UserFacade}
import com.learningobjects.cpxp.util.InternationalizationUtils as i18n
import com.learningobjects.de.group.Group
import com.typesafe.config as typesafe
import jakarta.servlet.http.HttpServletRequest
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.node.AssetNodeService
import loi.authoring.project.{AccessRestriction, ProjectService}
import loi.authoring.workspace.AttachedReadWorkspace
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.config.ConfigurationService
import loi.cp.course.CourseComponent.Init
import loi.cp.course.preview.{PreviewRole, PreviewService}
import loi.cp.localProxy.LocalProxy
import loi.cp.right.RightService
import loi.cp.role.{RoleService, SupportedRoleService}
import loi.cp.user.UserComponent
import scalaz.\/
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.boolean.*
import scaloi.syntax.boxes.*
import scaloi.syntax.instant.*
import scaloi.syntax.option.*

import java.util.UUID
import java.{lang as jl, util as ju}
import scala.compat.java8.OptionConverters.*
import scala.jdk.CollectionConverters.*
import scala.util.control.NoStackTrace

// N.B.: the "real" componentId of this is "loi.courseware.Courseware"
@Component
class LightweightCourseImpl(val self: LightweightCourseFacade)(
  assetService: AssetNodeService,
  val config: typesafe.Config,
  courseAccessService: CourseAccessService,
  courseConfigurationService: CourseConfigurationService,
  val environment: ComponentEnvironment,
  req: HttpServletRequest,
  rightService: RightService,
  supportedRoleService: SupportedRoleService,
  user: => UserDTO,
  workspaceService: ReadWorkspaceService,
  previewService: PreviewService,
  projectService: ProjectService,
  localProxy: LocalProxy,
)(implicit
  componentService: ComponentService,
  val configurationService: ConfigurationService,
  enrollmentWebService: EnrollmentWebService,
  fs: FacadeService,
  roleService: RoleService,
) extends Group(componentService, enrollmentWebService, rightService, self, supportedRoleService)
    with LightweightCourse
    with CourseDatesImpl
    with CourseVariables
    with ItemSiteComponent         // omfg
    with DateBasedCourseAccessImpl // omfg x2
    :

  import LightweightCourseImpl.*

  override def id: Long = self.getId

  @PostCreate
  def init(init: Init): Unit =
    val asset  = init.source.fold(_.loadCourse(), _._1)
    val branch = init.source.fold(_.loadBranch(), _._2)
    branch.project.foreach(p => self.setProjectId(p.id))
    self.setBranchId(branch.id)
    self.setLinkedAssetName(asset.info.name.toString)
    init.source.leftMap(o => self.setMasterCourseId(o.id))
    if !isPreviewSection then
      self.setGeneration(0L)
      setCommitId(init.source.fold(offering => offering.commitId, tuple => tuple._2.head.id))
      self.setLinkedAssetId(asset.info.id)
    CoursePermissions.initPermissions(this)
    self.setCreatedBy(init.createdBy.facade[UserFacade])
    init.startDate.ifPresent(sd => self.setStartDate(sd.asDate))
    init.endDate.ifPresent(ed => self.setEndDate(ed.asDate))
    self.setShutdownDate(init.shutdownDate)
    self.setGroupExternalId(init.externalId)
    self.setArchived(false)
  end init

  override def groupId: String = self.getGroupId

  override def externalId: Option[String] = self.getGroupExternalId.asScala

  private def isPreviewSection = getGroupType == GroupType.PreviewSection

  override def getName: String =
    if isPreviewSection then loadCourse().data.title else self.getName

  def getLogo: ImageFacade = self.getLogo

  def getSubtenantId: jl.Long = self.getSubtenant

  def setSubtenant(ˈsʌftɛnənt_aɪˈdiː: jl.Long): Unit =
    self.setSubtenant(ˈsʌftɛnənt_aɪˈdiː)

  def getPreferences: CoursePreferences = bowdlerize(courseConfigurationService.getGroupConfig(CoursePreferences, this))

  /** Bowdlerize the preferences for serialization to the front end. */
  private def bowdlerize(prefs: CoursePreferences): CoursePreferences =
    val rights         = courseAccessService.actualRights(this, user)
    val likeInstructor = rights.exists(_.likeInstructor)
    prefs.copy(
      CBLPROD16934InstructorResources = prefs.CBLPROD16934InstructorResources.when(likeInstructor),
      discussionReviewers = "",
      qnaCategories = prefs.qnaCategories.filter(_ => likeInstructor)
    )

  def getMasterCourseId = ju.Optional.empty()

  def setMasterCourse(id: Id): Unit = // ???
    throw new UnsupportedOperationException("setMasterCourse")

  override def isSelfStudy: Boolean = self.getSelfStudy.isTrue

  override def setSelfStudy(selfStudy: Boolean): Unit = self.setSelfStudy(selfStudy)

  override def getGeneration = isPreviewSection `flatNoption` self.getGeneration

  override def setGeneration(generation: Long): Unit = self.setGeneration(generation)

  override def getOffering: LightweightCourse = self.getMasterCourseId.component[LightweightCourse]

  override def isArchived: jl.Boolean = self.isArchived

  override def setArchived(archived: jl.Boolean): Unit = self.setArchived(archived)

  override def getUserRights = courseAccessService.getUserRights(this).asJava

  override def isRestricted = courseAccessService.isRestrictedLearner(this, user)

  override def setCourseId(id: Long): Unit =
    self.setLinkedAssetId(id)

  override def getCreatedBy: Option[UserDTO] =
    Option(self.getCreatedBy).map(UserDTO.apply)

  override def loadBranch(): Branch =
    projectService
      .loadBronch(self.getBranchId, AccessRestriction.none)
      .getOrElse(throw new RuntimeException(s"course section $getId has no branch"))

  override def commitId: Long =
    if isPreviewSection then // use the head commit of the branch
      loadBranch().head.id
    else                     // use the commit persisted with this section
      self.getCommitId.asScala.getOrElse(throw new RuntimeException(s"course section $getId has no commitId"))

  override def getCommitId: ju.Optional[jl.Long] = ju.Optional.of(commitId)

  override def setCommitId(id: Long): Unit =
    self.setCommitId(Option(id).boxInsideTo[ju.Optional]())

  override def getWorkspace: AttachedReadWorkspace =
    if isPreviewSection then workspaceService.requireReadWorkspace(self.getBranchId, AccessRestriction.none)
    else
      val commitId = getCommitId.get()
      workspaceService.requireReadWorkspaceAtCommit(self.getBranchId, commitId, AccessRestriction.none)

  override def loadCourse(): Asset[Course] =
    val ws       = getWorkspace
    val homeName = UUID.fromString(self.getLinkedAssetName)
    if isPreviewSection then // lookup the head version of the linked course
      assetService.loadA[Course](ws).byName(homeName).get
    else                     // lookup the persisted version of the linked course
      assetService
        .loadA[Course](ws)
        .byName(homeName)
        .getOrElse(throw new IllegalStateException(s"Course $getId has no primary root asset"))
  end loadCourse

  def delete(): Unit = self.delete()

  override def getOfferingId: Option[Long] = Option(self.getMasterCourseId).map(_.longValue())

  def renderSite(view: String): WebResponse =
    // Back in the day, the course would serve resources with relative urls like "assets/bar.css".
    // If the URL was "/Courses/foo" then this would be interpreted as "/Courses/bar.css" so we
    // redirected to "/Courses/foo/" for these resources to work. This is no longer a thing, and
    // LTI launch always gloms the / on anyway, but it seems imprudent to change this behaviour.
    val result = for
      _            <- (req.getPathInfo == getUrl) \/>! RedirectResponse.permanent(s"$getUrl/").widen
      instructorly <- checkAccess
      page          = instructorly.fold("instructor.html", "index.html")
      _            <- localProxy.proxyRequest(this, getUrl, req, Some(s"/$page")) <\/ ()
    yield HtmlResponse(this, page)
    result.merge
  end renderSite

  @Rpc
  def notFound(): WebResponse =
    // This serves requests to subpaths of the course URL, used to do learner/instructor preview
    val result = for
      _         <- previewRole.isLeft.flatOption(localProxy.proxyRequest(this, getUrl, req, None)) <\/ ()
      role      <- previewRole
      previewer <- previewUser(role)
      _          = Current.setUserDTO(previewer.toDTO)
      page       = if previewer == PreviewRole.Instructor then "instructor.html" else "index.html"
      _         <- localProxy.proxyRequest(this, getUrl, req, Some(s"/$page")) <\/ ()
    yield renderSite(null)
    result.merge
  end notFound

  private def previewRole: WebResponse \/ PreviewRole =
    val suffix = req.getPathInfo.substring(getUrl.length)
    PartialFunction.condOpt(suffix) {
      case "/learner"    => PreviewRole.Learner
      case "/instructor" => PreviewRole.Instructor
    } \/> ErrorResponse.notFound(suffix)

  private def previewUser(role: PreviewRole): WebResponse \/ UserComponent =
    previewService.findPreviewer(this, role) \/> ErrorResponse.badRequest(s"${role.entryName} preview user not found.")

  private def checkAccess: WebResponse \/ Boolean =
    for
      rights <- courseAccessService.actualRights(this, user) \/> notEnroledError
      _      <- (self.getDisabled && !rights.isAdministrator) \/>! courseSuspendedError
      _      <- (rights.likeInstructor `flatNoption` checkDateBasedAccess) <\/ (())
    yield rights.likeInstructor

  private def courseSuspendedError: WebResponse = forbidden(CourseSuspendedTitle, CourseSuspendedMessage)

  // TODO: This ought to distinguish between not enroled at all and expired enrolments.
  private def notEnroledError: WebResponse = forbidden(CourseNotEnroledTitle, CourseNotEnroledMessage)

  private def forbidden(titleKey: String, messageKey: String): ErrorResponse =
    val title = i18n.formatMessage(titleKey)
    val msg   = i18n.formatMessage(messageKey)
    ErrorResponse.forbidden(new UserException(title, msg) with NoStackTrace)
end LightweightCourseImpl

object LightweightCourseImpl:
  final val CourseSuspendedTitle    = "COURSE_SUSPENDED_TITLE"
  final val CourseSuspendedMessage  = "COURSE_SUSPENDED_MESSAGE"
  final val CourseNotEnroledTitle   = "COURSE_NOT_ENROLLED_TITLE"
  final val CourseNotEnroledMessage = "COURSE_NOT_ENROLLED_MESSAGE"
