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

package loi.cp.oauth.server

import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.HttpResponse
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.session.{SessionFacade, SessionService, SessionState}
import com.learningobjects.cpxp.service.user.{UserState, UserWebService}
import com.learningobjects.cpxp.util.{HttpUtils, SessionUtils}
import de.tomcat.juli.LogMeta
import jakarta.servlet.http.*
import loi.cp.user.UserComponent
import org.apache.commons.codec.binary.Base64
import scalaz.syntax.std.boolean.*

import java.nio.charset.StandardCharsets.UTF_8
import java.util.logging.{Level, Logger}
import scala.util.Try

@Service
class OAuthServiceImpl(sessionService: SessionService, userWebService: UserWebService, domain: => DomainDTO)(implicit
  cs: ComponentService
) extends OAuthService:
  import OAuthServiceImpl.*

  override def bearerAuthorize(request: HttpServletRequest): Option[HttpResponse] =
    request.getHeader("Authorization") match
      case BearerAuthorization(sessionId) =>
        val opt = for
          s <-
            Option(
              sessionService.lookupSession(sessionId, HttpUtils.getRemoteAddr(request, BaseServiceMeta.getServiceMeta))
            )
          u <- Option(s.getUser)
          if (u.getRootId == domain.id) && (u.getUserState == UserState.Active)
        yield
          Current.clearCache() // clear out any data cached for prior anon user
          Current.setUserDTO(userWebService.getUserDTO(u.getId))
          LogMeta.user(u.getId)
          LogMeta.username(Current.getUserDTO.userName)
          request.setAttribute(SessionUtils.REQUEST_PARAMETER_BEARER_AUTHORIZED, true)
          logger.log(Level.INFO, s"Bearer authorizing as: ${u.getUserName}")
          // yields Some[Unit] fwiw
        opt.fold(Option[HttpResponse](Unauthorized))(_ => None)

      case _ => None

  override def getUserForRequestWithOauthToken(request: HttpServletRequest): Option[UserComponent] =
    for
      sessionId <- getSessionId(request)
      session   <- getSessionForId(sessionId, request)
      user      <- userIsActive(session.getUser.component[UserComponent])
    yield user

  def userIsActive(user: UserComponent): Option[UserComponent] =
    (user.getUserState == UserState.Active).option(user)

  def getSessionId(req: HttpServletRequest): Option[String] =
    BearerAuthorization.unapply(req.getHeader("Authorization"))

  def getSessionForId(sessionId: String, req: HttpServletRequest): Option[SessionFacade] =
    Option(sessionService.lookupSession(sessionId, HttpUtils.getRemoteAddr(req, BaseServiceMeta.getServiceMeta)))
      .filter(_.getState == SessionState.Okay)
end OAuthServiceImpl

object OAuthServiceImpl:
  val logger = Logger.getLogger(classOf[OAuthServiceImpl].getName)

object BearerAuthorization:
  def unapply(h: String): Option[String] = h match
    case BearerB64(b64) if Base64.isBase64(b64) =>
      Try(
        new String(Base64.decodeBase64(b64), UTF_8) match
          case OAuthSession(s) => Some(s)
          case _               => None
      ).toOption.flatten
    case _                                      => None

  private val BearerB64    = """^Bearer\s+(\S+)$""".r
  private val OAuthSession = """^OAuth:(.*)$""".r
end BearerAuthorization

object Unauthorized extends HttpResponse:
  val statusCode                   = HttpServletResponse.SC_UNAUTHORIZED
  val headers: Map[String, String] = Map()
