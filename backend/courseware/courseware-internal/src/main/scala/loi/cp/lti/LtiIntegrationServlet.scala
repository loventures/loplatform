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
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.attachment.Disposition
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants.{
  ID_FOLDER_COURSE_OFFERINGS as OfferingFolder,
  ID_FOLDER_LIBRARIES as MasterCourseFolder
}
import loi.cp.Widen
import loi.cp.course.{CourseComponent, CourseFolderFacade}
import loi.cp.integration.ThinCommonCartridgeService
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scaloi.json.*
import scaloi.syntax.BooleanOps.*

import scala.compat.java8.OptionConverters.*

/** LTI integration API. These are URLs exposed in the admin portal.
  */
@Component
@ServletBinding(path = LtiIntegrationServlet.Path)
class LtiIntegrationServlet(val componentInstance: ComponentInstance)(implicit
  facadeService: FacadeService,
  thinCommonCartridgeService: ThinCommonCartridgeService
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import LtiIntegrationServlet.*
  import LtiError.*
  import ServletDispatcher.*

  override protected def handler: RequestHandler = {
    case RequestMatcher(Method.GET, MasterCoursePathCcXmlPath(groupId), _, _) =>
      findCourse(MasterCourseFolder, groupId).bimap(webError, ccXml(modules = false))

    case RequestMatcher(Method.GET, MasterCoursePathLtiXmlPath(groupId), _, _) =>
      findCourse(MasterCourseFolder, groupId).bimap(webError, ltiXml)

    case RequestMatcher(Method.GET, OfferingCcXmlPath(groupId), request, _) =>
      val modules = request.getParameter(ModulesParam) == "true"
      findCourse(OfferingFolder, groupId).bimap(webError, ccXml(modules))

    case RequestMatcher(Method.GET, OfferingLtiXmlPath(groupId), _, _) =>
      findCourse(OfferingFolder, groupId).bimap(webError, ltiXml)

    case RequestMatcher(method, path, _, _) =>
      webError(UnhandledRequest(method.name, path)).left
  }

  /** Get the common cartridge XML for a course. */
  private def ccXml(modules: Boolean)(course: CourseComponent): TextResponse =
    TextResponse
      .xml(thinCommonCartridgeService.getThinCommonCartridgeConfiguration(course, modules))
      .disposed(s"${course.getName}-${course.getGroupId}-lti.xml", Disposition.attachment)

  /** Get the LTI XML for a lightweight course. */
  private def ltiXml(course: CourseComponent): TextResponse =
    TextResponse
      .xml(thinCommonCartridgeService.getLtiXml(course))
      .disposed(s"${course.getName}-${course.getGroupId}-lti.xml", Disposition.attachment)

  /** Convert an Lti error to a web response. */
  private def webError(error: LtiError) = ArgoResponse(error, error.statusCode)

  /** Find a valid course by group id. */
  private def findCourse(folderId: String, groupId: String): LtiError \/ CourseComponent =
    for
      course <- findGroup(folderId, groupId) \/> CourseNotFound(groupId).widen
      _      <- course.getDisabled \/>! CourseSuspended(groupId)
    yield course

  /** Find a group by group id */
  private def findGroup(folderId: String, groupId: String): Option[CourseComponent] =
    folderId.facade[CourseFolderFacade].findCourseByGroupId(groupId).asScala
end LtiIntegrationServlet

object LtiIntegrationServlet:
  final val Path                               = "/api/lti1"
  private final val OfferingPath               = s"$Path/offering/([^/]+)"
  private final val MasterCoursePath           = s"$Path/masterCourse/([^/]+)"
  private final val OfferingCcXmlPath          = s"$OfferingPath/cc\\.xml".r
  private final val OfferingLtiXmlPath         = s"$OfferingPath/lti\\.xml".r
  private final val MasterCoursePathCcXmlPath  = s"$MasterCoursePath/cc\\.xml".r
  private final val MasterCoursePathLtiXmlPath = s"$MasterCoursePath/lti\\.xml".r

  private final val ModulesParam = "modules"

  private sealed abstract class LtiError(val statusCode: Int) extends Widen[LtiError]

  private object LtiError:
    import jakarta.servlet.http.HttpServletResponse.*

    final case class UnhandledRequest(method: String, path: String) extends LtiError(SC_BAD_REQUEST)
    final case class CourseNotFound(identifier: String)             extends LtiError(SC_NOT_FOUND)
    final case class CourseSuspended(identifier: String)            extends LtiError(SC_NOT_FOUND)

    implicit val LtiErrorEncodeJson: EncodeJson[LtiError] = Derivation.sumEncode[LtiError]("type"):
      case e: UnhandledRequest => "UnhandledRequest" -> EncodeJson.derive[UnhandledRequest].encode(e)
      case e: CourseNotFound   => "CourseNotFound"   -> EncodeJson.derive[CourseNotFound].encode(e)
      case e: CourseSuspended  => "UnhandledRequest" -> EncodeJson.derive[CourseSuspended].encode(e)
  end LtiError
end LtiIntegrationServlet
