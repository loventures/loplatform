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

package loi.cp.structure

import argonaut.*
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.Widen
import loi.cp.content.CourseContentService
import loi.cp.integration.IntegrationService
import loi.cp.right.RightService
import scalaz.\/
import scalaz.syntax.bind.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scaloi.json.*
import scaloi.syntax.boolean.*
import scaloi.syntax.regex.*

import jakarta.servlet.http.HttpServletRequest

/** Course structure API.
  */
@Component
@ServletBinding(path = CourseStructureServlet.Path)
class CourseStructureServlet(val componentInstance: ComponentInstance)(implicit
  facadeService: FacadeService,
  structureService: CourseStructureService,
  courseContentService: CourseContentService,
  integrationService: IntegrationService,
  rightService: RightService,
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import CourseStructureServlet.*
  import ServletDispatcher.*
  import StructureError.*

  override protected def handler: RequestHandler = {
    case RequestMatcher(Method.GET, OfferingPath(groupId), request, _) =>
      (checkAccess(request) >> structureService.getOfferingStructure(groupId)).bimap(webError, structureResponse)

    case RequestMatcher(Method.GET, SectionPath(externalId), request, _) =>
      (checkAccess(request) >> structureService.getCourseStructure(externalId)).bimap(webError, structureResponse)

    case RequestMatcher(method, path, _, _) =>
      webError(UnhandledRequest(method.name, path)).left
  }

  private def checkAccess(request: HttpServletRequest): StructureError \/ Unit =
    for
      auth   <- request.header("Authorization") \/> AccessDenied()
      key    <- BearerAuthRE.match1(auth) \/> AccessDenied()
      apiKey <- Option(integrationService.getApiKeyBySecret(key, request.getRemoteAddr)) \/> AccessDenied()
      _      <- apiKey.getRightClasses.contains(classOf[ReadStructureRight]) \/> AccessDenied()
    yield ()

  /** Get the structure for a lightweight course or offering. */
  private def structureResponse(structure: Structure): ArgoResponse[Structure] =
    ArgoResponse(structure)

  /** Convert a structure error to a web response. */
  private def webError(error: StructureError): ArgoResponse[StructureError] =
    ArgoResponse(error, error.statusCode)
end CourseStructureServlet

object CourseStructureServlet:
  private final val logger = org.log4s.getLogger

  final val Path                 = "/api/structure"
  private final val OfferingPath = s"$Path/offering/([^/]+)".r
  private final val SectionPath  = s"$Path/section/([^/]+)".r
  private final val BearerAuthRE = """Bearer\s+(\S+)""".r

sealed abstract class StructureError(val statusCode: Int) extends Widen[StructureError]

final case class StructureException(error: StructureError) extends Exception(error.toString)

object StructureError:
  import jakarta.servlet.http.HttpServletResponse.*

  final case class UnhandledRequest(method: String, path: String) extends StructureError(SC_BAD_REQUEST)
  final case class CourseNotFound(identifier: String)             extends StructureError(SC_NOT_FOUND)
  final case class InternalError(identifier: String)              extends StructureError(SC_INTERNAL_SERVER_ERROR)
  final case class AccessDenied()                                 extends StructureError(SC_FORBIDDEN)

  implicit val structureErrorEncodeJson: EncodeJson[StructureError] = Derivation.sumEncode[StructureError]("type"):
    case e: UnhandledRequest => "UnhandledRequest" -> EncodeJson.derive[UnhandledRequest].encode(e)
    case e: CourseNotFound   => "CourseNotFound"   -> EncodeJson.derive[CourseNotFound].encode(e)
    case e: InternalError    => "InternalError"    -> EncodeJson.derive[InternalError].encode(e)
    case e: AccessDenied     => "AccessDenied"     -> EncodeJson.derive[AccessDenied].encode(e)
end StructureError
