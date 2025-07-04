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

package loi.cp.session

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.session.SessionService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.HttpUtils.*
import com.learningobjects.cpxp.util.SessionUtils.COOKIE_NAME_SESSION
import jakarta.servlet.http.HttpServletRequest
import org.apache.http.HttpHeaders
import scalaz.syntax.either.*
import scaloi.misc.Extractor

import scala.concurrent.duration.*

@Component
@ServletBinding(path = SessionServlet.Path)
class SessionServlet(val componentInstance: ComponentInstance)(implicit
  iws: IntegrationWebService,
  om: ObjectMapper,
  ss: SessionService,
  user: UserDTO,
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:

  import ServletDispatcher.*
  import SessionServlet.*

  protected def handler: RequestHandler = {
    case RequestMatcher(Method.GET, Path, req, rsp)  =>
      setExpired(rsp)
      EntityResponse(sessionStatus(req)).right
    case RequestMatcher(Method.POST, Path, req, rsp) =>
      val remoteAddress = getRemoteAddr(req, BaseServiceMeta.getServiceMeta)
      val session       = ss.lookupSession(sessionId(req), remoteAddress)
      if session ne null then ss.pingSession(sessionId(req), remoteAddress)
      // Meh, return same as GET...
      setExpired(rsp)
      EntityResponse(sessionStatus(req)).right
  }

  private def sessionId(req: HttpServletRequest) =
    getCookieValue(req, COOKIE_NAME_SESSION)

  private def sessionStatus(req: HttpServletRequest): SessionStatus =
    ss.getSessionValidity(sessionId(req)) match
      case validity if validity > 0 => Valid(validity)
      case _ if isSystemUser(req)   => Valid.forever
      case _                        => Invalid

  private def isSystemUser(req: HttpServletRequest): Boolean =
    Option(req.getHeader(HttpHeaders.AUTHORIZATION)) match
      case Some(Bearer(key)) => !Option(iws.getByKey(key)).forall(_.getDisabled)
      case _                 => false
end SessionServlet

object SessionServlet:
  final val Path = "/api/v0/session"

  val Bearer = Extractor `dropPrefix` (AUTHORIZATION_BEARER + " ")

  private sealed abstract class SessionStatus(val valid: Boolean) extends Product with Serializable
  private case object Invalid                                     extends SessionStatus(valid = false)
  private final case class Valid(expires: Long)                   extends SessionStatus(valid = true)
  private object Valid:
    val forever: SessionStatus = Valid(10.days.toMillis)
end SessionServlet
