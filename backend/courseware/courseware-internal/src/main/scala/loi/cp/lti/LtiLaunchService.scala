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

package loi.cp.lti

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN as Forbidden
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.content.{
  ContentAccessService,
  ContentDeletedException,
  CourseAlreadyShutdownException,
  CourseContent,
  CourseNotYetStartedException
}
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.{CourseSection, CourseSectionService}
import loi.cp.integration.BasicLtiSystemComponent
import loi.cp.reference.EdgePath
import loi.cp.user.UserComponent
import scalaz.\/
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.syntax.any.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Responsible for processing different types of launch request.
  */
@Service
class LtiLaunchService(
  contentAccessService: ContentAccessService,
  coursewareAnalyticsService: CoursewareAnalyticsService,
  courseSectionService: CourseSectionService,
  ltiCourseService: LtiCourseService,
  ltiEnrolmentService: LtiEnrolmentService,
  ltiOutcomesService: LtiOutcomesService,
  ltiSessionService: LtiSessionService,
  ltiSubtenantService: LtiSubtenantService,
  ltiUserService: LtiUserService
):
  import LtiLaunchService.*

  /** Launch to an absolute URL in the system.
    */
  def launchUrl(pi: Option[String])(implicit
    request: HttpServletRequest,
    response: HttpServletResponse,
    system: BasicLtiSystemComponent
  ): LtiError \/ String =
    for
      url          <- pi.cata(decodePathInfo, ltiParam_!(CustomUrlParameter))
      subtenantOpt <- ltiSubtenantService.processSubtenant
      user         <- ltiUserService.processUser(subtenantOpt)
      _            <- ltiEnrolmentService.processRoles(user)
      _            <- ltiSessionService.login(user)
    yield url

  private def decodePathInfo(pi: String): LtiError \/ String =
    "/".concat(URLDecoder.decode(pi, StandardCharsets.UTF_8.name)).right

  /** Launch into a course section. Optionally launch with an activityPath to direct to that specific activity content.
    */
  def launchCourse(courseIdentifier: CourseIdentifier, activityPath: Option[String])(implicit
    request: HttpServletRequest,
    response: HttpServletResponse,
    system: BasicLtiSystemComponent
  ): LtiError \/ String =
    for
      subtenantOpt <- ltiSubtenantService.processSubtenant
      user         <- ltiUserService.processUser(subtenantOpt)
      courseGoc    <-
        courseIdentifier.fold(ltiCourseService.provisionContext(_, subtenantOpt), ltiCourseService.findSection)
      faculty      <- ltiEnrolmentService.processRoles(user, courseGoc.result)
      course       <- contentAccessService.getCourse(courseGoc.result.id, user.userId, faculty) \/> accessError
      content      <- activityPath.traverse(validateEdgePath(course, user, _))
      _            <- ltiSessionService.login(user)
      urlOpt       <- ltiParam(CustomUrlParameter)
      previewAsOpt <- ltiParam(CustomPreviewAsParameter)
      presentOpt   <- ltiParamT[LtiPresentationMode](CustomPresentationModeParameter)
      previewOpt   <- previewAsOpt.traverse(validateStudent)
      section      <- loadCourseSection(courseGoc.result.id)
      _            <- ltiOutcomesService.processLaunch(section, user, content, system.getId, faculty)
    yield

      // the strings "instructor" and "student" are both role types for the course-lw app
      // and the role ids (see EnrollmentWebService.STUDENT_ROLE_ID, etc)
      val roleType = faculty.fold("instructor", "student")
      coursewareAnalyticsService.emitSectionEntryEvent(section.id, roleType, None)

      def coursePath: String =
        val presentationMode = presentOpt | LtiPresentationMode.Meek
        val isMeek           = presentationMode != LtiPresentationMode.Normal
        val rootParam        = isMeek.option(s"contentItemRoot=${activityPath.getOrElse("_root_")}")
        val previewParam     = previewOpt.map(u => s"previewAsUserId=${u.id}")
        val isObsequious     = presentationMode == LtiPresentationMode.Obsequious
        val headerParam      = isObsequious.option("noHeader")
        val params           = List(rootParam, previewParam, headerParam).flatten ??> (_.mkString("?", "&", ""))
        if previewOpt.isDefined then s"/#/student/content/${activityPath.orZ}$params"
        else activityPath.cata(path => s"/#/$roleType/content/$path$params", s"/#/index$params")
      end coursePath

      urlOpt.cata(_.replace(UrlIdToken, courseGoc.result.id.toString), course.getUrl + coursePath)

  private def loadCourseSection(id: Long): LtiError \/ CourseSection =
    lazy val msg = s"could not load course section $id"
    courseSectionService.getCourseSection(id) \/> InternalLtiError(msg, new RuntimeException(msg))

  /** Validate an edge path within the context of a course. */
  private def validateEdgePath(
    course: LightweightCourse,
    user: UserComponent,
    path: String
  ): LtiError \/ CourseContent =
    contentAccessService.getContentReadOnly(course, EdgePath.parse(path), user.userId) \/> accessError

  /** Validate that a user is known. */
  private def validateStudent(
    userId: String
  )(implicit system: BasicLtiSystemComponent): LtiError \/ UserComponent =
    ltiUserService.findUser(userId) \/> GenericLtiError("lti_unknown_preview_user", userId)

  /** Transform the content access error non-algebra into the LTI error algebra. */
  private def accessError(ex: Throwable): LtiError =
    ex tap { e =>
      logger.warn(e)("Access denied during LTI launch")
    } match
      case _: ContentDeletedException           =>
        FriendlyLtiError("lti_content_deleted", Forbidden)
      case _: ResourceNotFoundException         =>
        GenericLtiError("lti_unknown_path")
      case CourseNotYetStartedException(when)   =>
        FriendlyLtiError("lti_section_not_yet_started", when, Forbidden)
      case CourseAlreadyShutdownException(when) =>
        FriendlyLtiError("lti_section_already_shutdown", when, Forbidden)
      case _: AccessForbiddenException          =>
        FriendlyLtiError("lti_access_denied", Forbidden)
      case o                                    =>
        InternalLtiError("lti_internal_error", o)
end LtiLaunchService

object LtiLaunchService:
  private final val logger = org.log4s.getLogger

  private final val CustomUrlParameter              = "custom_url"
  private final val CustomPreviewAsParameter        = "custom_preview_as"
  private final val CustomPresentationModeParameter = "custom_presentation_mode"

  final val UrlIdToken = "_ID_"

  /** Identifies the course into which to launch. */
  sealed trait CourseIdentifier:
    def fold[A](fOffering: String => A, fSection: (String, String) => A): A =
      this match
        case Offering(offeringId)          => fOffering(offeringId)
        case Section(folderId, externalId) => fSection(folderId, externalId)

  /** Provision from a course offering. */
  final case class Offering(offeringId: String) extends CourseIdentifier

  /** Launch to an existing section. */
  final case class Section(folderId: String, externalId: String) extends CourseIdentifier
end LtiLaunchService
