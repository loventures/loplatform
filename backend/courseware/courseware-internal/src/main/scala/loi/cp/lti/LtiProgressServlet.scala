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

import argonaut.*
import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse.*
import loi.cp.course
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.{CourseSection, CourseSectionService}
import loi.cp.enrollment.EnrollmentService
import loi.cp.lti.*
import loi.cp.lwgrade.GradeService
import loi.cp.progress.LightweightProgressService
import loi.cp.reference.EdgePath
import loi.cp.user.*
import scalaz.\/
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.json.ArgoExtras

/** LTI Progress API. GET /api/lti/progress?oauth_consumer_key=...&oauth_timestamp=...&oauth_nonce=...
  * &context_id=...&user_id=...
  *
  * Returns empty progress for unknown section, unknown user and unenroled user as all these are plausibly valid
  * pre-first-launch scenarios. We also return progress for finished enrolments/sections as these too are not unlawful.
  */
@Component
@ServletBinding(path = LtiProgressServlet.Path)
class LtiProgressServlet(val componentInstance: ComponentInstance, gradeService: GradeService)(implicit
  ltiValidationService: LtiValidationService,
  ltiCourseService: LtiCourseService,
  ltiUserService: LtiUserService,
  courseSectionService: CourseSectionService,
  enrolmentService: EnrollmentService,
  lightweightProgressService: LightweightProgressService
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import LtiProgressServlet.*
  import ServletDispatcher.*

  override protected def handler: RequestHandler = {
    case RequestMatcher(Method.GET, Path, request, _) =>
      handleProgressRequest(request).bimap(fromLtiError, ArgoResponse(_))

    case RequestMatcher(method, path, _, _) =>
      rthError("rth_invalid_request", s"${method.name} $path".some).left
  }

  private def handleProgressRequest(request: HttpServletRequest): LtiError \/ LtiProgressResponse =
    for
      system     <- ltiValidationService.validateOAuthRequest(using request)
      userOpt    <- ltiUserService.getUser(using request, system)
      contextOpt <- userOpt.cata(
                      ltiCourseService.getMostRecentContext(using request, system, _),
                      Option.empty[course.lightweight.LightweightCourse].right[LtiError]
                    )
      sectionOpt <- contextOpt.traverse(loadSection)
      percent     = getProgressPercent(userOpt, sectionOpt)
    yield LtiProgressResponse(percent)

  private def loadSection(course: LightweightCourse): LtiError \/ CourseSection =
    courseSectionService.getCourseSection(course.id) \/> GenericLtiError("lti_invalid_section", course.groupId)

  // Don't care about actual current enrolment status, we want your progress if it exists
  // so you can see your progress on a course that you already completed, for example. But
  // if you don't exist or never enrolled then return nothing.
  private def getProgressPercent(
    userOpt: Option[UserComponent],
    sectionOpt: Option[CourseSection]
  ): Option[Double] =
    for
      user       <- userOpt
      section    <- sectionOpt
      if enrolmentService.loadEnrollments(user.id, section.id, EnrollmentType.ALL).nonEmpty
      gradebook   = gradeService.getGradebook(section, user)
      progressMap = lightweightProgressService.loadProgress(section, user, gradebook)
      progress   <- progressMap.get(EdgePath.Root)
    yield progress.weightedPercentage

  private def fromLtiError(error: LtiError): ArgoResponse[LtiProgessError] = error match
    case e @ MissingLtiParameter(name)    => rthError(e.msg, name.some)
    case e @ InvalidLtiParameter(name, _) => rthError(e.msg, name.some)
    case GenericLtiError(msg, param)      => rthError(msg, Option(param))
    case FriendlyLtiError(msg, param, sc) => rthError(msg, Option(param).map(_.toString), sc)
    case e                                => rthError(e.msg, None)
end LtiProgressServlet

object LtiProgressServlet:
  final val Path = "/api/lti/progress"

  def rthError(
    reason: String,
    detail: Option[String],
    sc: Int = SC_BAD_REQUEST
  ): ArgoResponse[LtiProgessError] =
    ArgoResponse(LtiProgessError(reason, detail), sc)

  // None progress occurs when the user or section does not exist or the user is not enrolled.
  // Once you have launched, you will receive a token 0 progress even if you have done nothing.
  case class LtiProgressResponse(progress: Option[Double])

  object LtiProgressResponse:
    implicit val ltiProgressResponseEncodeJson: CodecJson[LtiProgressResponse] =
      CodecJson.casecodec1(LtiProgressResponse.apply, ArgoExtras.unapply1)("progress")

  case class LtiProgessError(reason: String, detail: Option[String])

  object LtiProgessError:
    implicit val ltiProgressErrorEncodeJson: CodecJson[LtiProgessError] =
      CodecJson.casecodec2(LtiProgessError.apply, ArgoExtras.unapply)("reason", "detail")

end LtiProgressServlet
