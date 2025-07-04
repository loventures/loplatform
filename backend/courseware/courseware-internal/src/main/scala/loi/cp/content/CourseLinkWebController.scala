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

package loi.cp.content

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method, RedirectResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Summon.summon
import com.learningobjects.cpxp.service.enrollment.EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.{
  EnrollmentType,
  INSTRUCTOR_ROLE_ID,
  STUDENT_ROLE_ID
}
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType.{CourseSection, PreviewSection, TestSection}
import com.learningobjects.cpxp.service.group.GroupConstants.{
  DATA_TYPE_GROUP_BRANCH,
  ID_FOLDER_COURSES,
  ID_FOLDER_TEST_SECTIONS
}
import com.learningobjects.cpxp.service.query.Comparison
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import com.learningobjects.de.authorization.securedByImplementation
import loi.asset.course.model.Course
import loi.asset.external.CourseLink
import loi.asset.external.SectionPolicy.{LinkedSection, MostRecent}
import loi.authoring.asset.Asset
import loi.authoring.node.AssetNodeService
import loi.authoring.project.AccessRestriction
import loi.authoring.workspace.AttachedReadWorkspace
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.context.ContextId
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.course.lightweight.{LightweightCourse, LightweightCourseService}
import loi.cp.course.preview.PreviewWebController.CoursePkHeader
import loi.cp.course.preview.{PreviewRole, PreviewService, PreviewWebController}
import loi.cp.course.{CourseAccessService, CourseComponent, CourseFolderFacade}
import loi.cp.enrollment.EnrollmentService
import loi.cp.lti.storage.{CourseLinkData, UserGradeSyncHistory}
import loi.cp.offering.ProjectOfferingService
import loi.cp.reference.EdgePath
import loi.cp.role.{RoleService, RoleType}
import loi.cp.storage.CourseStorageService
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*

import java.lang
import java.util.UUID
import scala.jdk.OptionConverters.*

@securedByImplementation
@Controller(root = true)
trait CourseLinkWebController extends ApiRootComponent:
  @RequestMapping(path = "lwc/{context}/course/{path}/launch", method = Method.GET)
  def internalRedirect(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
    @QueryParam("role") role: Option[PreviewRole],
  ): ErrorResponse \/ RedirectResponse

@Component
class CourseLinkWebControllerImpl(val componentInstance: ComponentInstance)(implicit
  courseAccessService: CourseAccessService,
  contentAccessService: ContentAccessService,
  courseStorageService: CourseStorageService,
  coursewareAnalyticsService: CoursewareAnalyticsService,
  enrollmentService: EnrollmentService,
  enrollmentService2: EnrollmentWebService, // ffs
  roleService: RoleService,
  workspaceService: ReadWorkspaceService,
  nodeService: AssetNodeService,
  offeringService: ProjectOfferingService,
  lwcService: LightweightCourseService,
  previewService: PreviewService,
  facadeService: FacadeService,
  user: UserDTO,
) extends CourseLinkWebController
    with ComponentImplementation:
  import CourseLinkWebControllerImpl.*

  // TODO This really needs friendly error support and all that.. It could
  // be a POST from the FE that returns an error or redirect URL, then the
  // FE would get to display the error. Open in new window then becomes
  // author config. But that breaks the prior statement.

  override def internalRedirect(
    context: Long,
    path: EdgePath,
    role: Option[PreviewRole]
  ): ErrorResponse \/ RedirectResponse =
    for
      (course, _, asset) <- contentAccessService.useContentT[CourseLink](context, path, user).disjoin(accessError)
      access             <- courseAccessService.actualRights(course, user) \/> ErrorResponse.forbidden
      branchId           <- asset.data.branch \/> ErrorResponse.serverError("No branch configured")
      ws                  = workspaceService.requireReadWorkspace(branchId, AccessRestriction.none)
      redirect           <- course.getGroupType match
                              case PreviewSection => previewRedirect(course, path, asset, access, ws, role)
                              case TestSection    => testRedirect(course, path, asset, access, ws)
                              case _              => courseRedirect(course, path, asset, access, ws)
    yield redirect

  // Handles internal redirect from one preview section to another.
  // We can't just redirect to the preview controller endpoint
  // because in the student preview case we need to set up the
  // grade callback.
  private def previewRedirect(
    course: LightweightCourse,
    path: EdgePath,
    asset: Asset[CourseLink],
    access: CourseRights,
    ws: AttachedReadWorkspace,
    role: Option[PreviewRole],
  ): ErrorResponse \/ RedirectResponse =
    val pwc = summon[PreviewWebController]
    for redirect <- pwc.previewCourse(ws.bronchId, ws.homeName, "", role)
    yield
      if role.contains(PreviewRole.Learner) && asset.data.gradable then
        // I am the real my but I need grades to flow back to my preview student user who is
        // unique to the origin section...
        previewService.findPreviewer(course, PreviewRole.Learner) foreach { previewer =>
          // I'm just taking advantage of PreviewWebController but need to know the preview section
          // id and, well, this is a way to do it.
          val sectionId = redirect.headers(PreviewWebController.CoursePkHeader).toLong
          val studentId = redirect.headers(PreviewWebController.UserPkHeader).toLong
          courseStorageService.modify[UserGradeSyncHistory](ContextId(sectionId), UserId(studentId))(
            _.copy(courseLinkData = Some(CourseLinkData(course.id, path, Some(previewer.id))))
          )
        }
      end if
      redirect
    end for
  end previewRedirect

  private def testRedirect(
    from: LightweightCourse,
    path: EdgePath,
    asset: Asset[CourseLink],
    access: CourseRights,
    ws: AttachedReadWorkspace,
  ): ErrorResponse \/ RedirectResponse =
    for section <- asset.data.sectionPolicy match
                     case MostRecent    =>
                       mostRecentSection(branchTestSections(ws.bronchId)) \/> ErrorResponse.serverError(
                         "No sections"
                       )
                     case LinkedSection =>
                       linkedSection(from, ws, None)
    yield
      initEnrolment(from, section, path, asset, access)
      RedirectResponse.temporary(section.getUrl).copy(headers = Map(CoursePkHeader -> section.id.toString))

  private def courseRedirect(
    from: LightweightCourse,
    path: EdgePath,
    asset: Asset[CourseLink],
    access: CourseRights,
    ws: AttachedReadWorkspace,
  ): ErrorResponse \/ RedirectResponse =
    for
      offering <- offeringService.getOfferingComponentForBranch(ws.branch) \/> ErrorResponse.serverError("No offering")
      section  <- asset.data.sectionPolicy match
                    case MostRecent    =>
                      mostRecentSection(offeringService.getCourseSections(offering)) \/> ErrorResponse.serverError(
                        "No sections"
                      )
                    case LinkedSection =>
                      linkedSection(from, ws, offering.some)
    yield
      initEnrolment(from, section, path, asset, access)
      RedirectResponse.temporary(section.getUrl).copy(headers = Map(CoursePkHeader -> section.id.toString))

  // Returns a section that you are or were enroled in, or else the most recent section.
  private def mostRecentSection(sections: Seq[LightweightCourse]): Option[LightweightCourse] =
    val enroledIds = enrollmentService2
      .getUserEnrollmentsQuery(user.id, EnrollmentType.ALL)
      .addCondition(DATA_TYPE_ENROLLMENT_GROUP, Comparison.in, sections.map(_.id))
      .setDataProjection(DATA_TYPE_ENROLLMENT_GROUP)
      .getValues[lang.Long]
    sections.find(section => enroledIds.contains(section.id)) || sections.sortBy(_.createDate).lastOption

  private def initEnrolment(
    from: LightweightCourse,
    to: LightweightCourse,
    path: EdgePath,
    asset: Asset[CourseLink],
    access: CourseRights,
  ): Unit =
    if !access.isAdministrator then
      val roleId     = if access.likeInstructor then INSTRUCTOR_ROLE_ID else STUDENT_ROLE_ID
      val role       = roleService.findRoleByRoleId(to, roleId).toScala.map(RoleType.apply).get
      val enrolments = enrollmentService.loadEnrollments(user.id, from.id, EnrollmentType.ACTIVE_ONLY)
      val startTime  = enrolments.flatMap(e => Option(e.getStartTime)).minOption
      val stopTime   = enrolments.flatMap(e => Option(e.getStopTime)).maxOption
      logger.info(s"Enrolling ${user.userName} in ${to.getGroupId} as $roleId")
      enrollmentService.setEnrollment(
        user,
        to,
        role,
        Some(s"CourseLink:${from.getGroupId}"),
        startTime,
        stopTime
      )
      if asset.data.gradable && !access.likeInstructor then
        courseStorageService.modify[UserGradeSyncHistory](to, user)(
          _.copy(courseLinkData = Some(CourseLinkData(from.id, path, None)))
        )
      coursewareAnalyticsService.emitSectionEntryEvent(to.id, roleId, Some(from.id))

  private def linkedSection(
    from: LightweightCourse,
    ws: AttachedReadWorkspace,
    offering: Option[LightweightCourse],
  ): ErrorResponse \/ LightweightCourse =
    for course <- nodeService.loadA[Course](ws).byName(ws.homeName) \/> ErrorResponse.internalError
    yield
      // using a hacky external id to link these is lame but low effort. The
      // downside is we could never link to another externally-created section
      val externalId = s"CourseLink:${from.externalId | from.groupId}:${ws.bronchId}"
      val init       = new CourseComponent.Init(
        name = course.data.title,
        groupId = UUID.randomUUID.toString,
        groupType = if offering.isDefined then CourseSection else TestSection,
        createdBy = null, // ooph
        source = offering <\/ (course -> ws.branch),
        disabled = false,
        externalId = Some(externalId).toJava,
        startDate = from.getStartDate.toJava,
        endDate = from.getEndDate.toJava,
        shutdownDate = from.getShutdownDate.toJava,
      )
      (if offering.isDefined then courseSectionsFolder else testSectionsFolder)
        .getOrCreateCourseByExternalId(
          externalId,
          LightweightCourse.Identifier,
          init
        )
        .map(_.asInstanceOf[LightweightCourse])
        .init(course =>
          logger.info(s"Created section ${course.id}")
          Option(from.getSubtenantId).foreach(course.setSubtenant)
          lwcService.initializeSection(course, offering)
        )
        .result

  private def branchTestSections(branchId: Long): Seq[LightweightCourse] =
    testSectionsFolder.queryGroups
      .addCondition(DATA_TYPE_GROUP_BRANCH, Comparison.eq, branchId)
      .getComponents[LightweightCourse]

  private def testSectionsFolder: CourseFolderFacade =
    ID_FOLDER_TEST_SECTIONS.facade[CourseFolderFacade]

  private def courseSectionsFolder: CourseFolderFacade =
    ID_FOLDER_COURSES.facade[CourseFolderFacade]
end CourseLinkWebControllerImpl

object CourseLinkWebControllerImpl:
  private final val logger = org.log4s.getLogger

  def accessError(t: Throwable): ErrorResponse = t match
    case forbidden: AccessForbiddenException => ErrorResponse.forbidden(forbidden.getMessage)
    case notFound: ResourceNotFoundException => ErrorResponse.notFound(notFound.getMessage)
    case badRequest                          => ErrorResponse.badRequest(badRequest.getMessage)
