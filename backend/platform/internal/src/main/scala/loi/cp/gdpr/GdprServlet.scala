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

package loi.cp.gdpr

import argonaut.Parse
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.Method.{GET, POST}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import jakarta.servlet.http.HttpServletRequest
import loi.cp.overlord.EnforceOverlordAuth
import scalaz.\/
import scalaz.syntax.either.*
import scaloi.json.ArgoExtras.*

@Component
@ServletBinding(path = GdprServlet.SysGdpr, system = true)
@EnforceOverlordAuth
class GdprServlet(val componentInstance: ComponentInstance, gdprService: GdprService)
    extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import GdprServlet.*
  import ServletDispatcher.*

  override def handler: RequestHandler = {
    case RequestMatcher(GET, SysGdpr, _, _) =>
      ArgoResponse("GDPR").right

    case RequestMatcher(POST, Lookup, request, _) =>
      parse(request).map(lookup)

    case RequestMatcher(POST, Obfuscate, request, _) =>
      parse(request).map(obfuscate)
  }

  private def parse(request: HttpServletRequest): ErrorResponse \/ Emails =
    Parse.decode_\/[Emails](request.body).leftMap(unprocessable)

  private def unprocessable(s: String): ErrorResponse =
    logger.info(s"Error parsing JSON: $s")
    ErrorResponse.unprocessable(s)

  private def lookup(emails: Emails): WebResponse = ArgoResponse(gdprService.lookup(emails, true))

  private def obfuscate(emails: Emails): WebResponse = ArgoResponse(gdprService.obfuscate(emails))
end GdprServlet

object GdprServlet:
  private final val logger = org.log4s.getLogger

  final val SysGdpr           = "/sys/gdpr"
  private final val Lookup    = s"$SysGdpr/lookup"
  private final val Obfuscate = s"$SysGdpr/obfuscate"
