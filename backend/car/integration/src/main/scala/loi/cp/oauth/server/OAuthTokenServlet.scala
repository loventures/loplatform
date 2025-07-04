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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.component.web.Method.POST
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.login.LoginWebService
import com.learningobjects.cpxp.service.login.LoginWebService.LoginStatus
import com.learningobjects.cpxp.service.session.SessionService
import com.learningobjects.cpxp.service.user.{UserState, UserWebService}
import jakarta.servlet.http.HttpServletRequest
import loi.cp.apikey.ApiKeySystem
import loi.cp.oauth.TokenResponse
import loi.cp.oauth.server.AuthErrors.*
import org.apache.commons.codec.binary.Base64
import scalaz.*
import scalaz.syntax.std.option.*

import java.nio.charset.StandardCharsets.UTF_8

/** A servlet that implements some parts of some OAuth specs around bearer token issuance.
  *
  * https://tools.ietf.org/html/rfc6749
  */
@Component
@ServletBinding(path = OAuthTokenServlet.Url)
class OAuthTokenServlet(
  val componentInstance: ComponentInstance,
  uws: UserWebService,
  lws: LoginWebService,
  ss: SessionService
) extends ServletComponent
    with ComponentImplementation
    with ServletDispatcher:
  import OAuthTokenServlet.*
  import ServletDispatcher.*

  val TokenPrefix = "OAuth:"

  /** Http request parameter names. */
  object Parameters:
    val GrantType = "grant_type"
    val Username  = "username"
    val Password  = "password"
    val Scope     = "scope"

  /** Supported token grant types. */
  object GrantTypes:

    /** Requests a token for a specified user, where the user is prompted for their username/password.
      */
    val Password = "password"

    /** Requests a token for a specified user, assuming the request is protected by an api key with sufficient
      * privileges.
      */
    val Username = "username"

    /** Requests a token for an api key, assuming the request is protected by that api key's secret.
      */
    val ClientCredentials = "client_credentials"
  end GrantTypes

  /** Supported issued token types */
  object TokenTypes:
    val Bearer = "Bearer"

  /** Process a post request to the token endpoint. */
  override def handler: RequestHandler = { case RequestMatcher(POST, Url, request, _) =>
    for
      system <- auth(request)
      user   <- grant(request, system) \/> authError(InvalidGrant)
      token  <- tokenResponse(user) \/> authError(ServerError)
    yield TextResponse.json(ComponentUtils.toJson(token))
  }

  private def auth(request: HttpServletRequest): ErrorResponse \/ ApiKeySystem =
    BasicAuth
      .validateAuthorization(request)
      .leftMap(e => unauthorized(e)) // upon basic auth failure, issue a 401 response

  private def authError(error: AuthError) = ErrorResponse.badRequest.copy(body = Some(error))

  /** Processes the grant request.
    *
    * @return
    *   The user id of the grant, if granted, none if not granted.
    */
  private def grant(request: HttpServletRequest, system: ApiKeySystem): Option[Long] =
    request.getParameter(Parameters.GrantType) match
      case GrantTypes.Password =>
        processPasswordRequest(request)

      case GrantTypes.Username =>
        processSudoRequest(request)

      case GrantTypes.ClientCredentials =>
        processClientRequest(system)

      case _ =>
        None

  // TODO: verify login right
  // TODO: switch to using session root to engage in lockout flow etc
  // TODO: prevent privilege escalation

  /** Attempt a username/password login on behalf of the remote system. Return auth error or token response.
    */
  private def processPasswordRequest(request: HttpServletRequest): Option[Long] =
    for
      un   <- request.param(Parameters.Username)
      pw   <- request.param(Parameters.Password)
      login = lws.authenticateExternal(un, pw)
      if login.status == LoginStatus.OK
    yield login.userId.longValue

  // TODO: verify sudo right
  // TODO: prevent privilege escalation

  /** Assume a specified user's identity on behalf of the remote system. Return auth error or token response.
    */
  private def processSudoRequest(request: HttpServletRequest): Option[Long] =
    for
      un <- request.param(Parameters.Username)
      f  <- Option(uws.getUserByUserName(un))
      if f.getUserState == UserState.Active
    yield f.getId.longValue

  /** Assume the remote system's identity. Return auth error or token response.
    */
  private def processClientRequest(system: ApiKeySystem): Option[Long] =
    Some(system.getSystemUser).map(_.getId)

  /** Transform an option user into an error or token response. */
  private def tokenResponse(user: Long): Option[TokenResponse] =
    for
      session     <- Option(ss.openSession(user, false, null))
      sessionValue = TokenPrefix + session.getSessionId
      sessionId    = Base64.encodeBase64String(sessionValue.getBytes(UTF_8))
    yield
      // TODO: accurate lifetime
      // TODO: token reissue token
      TokenResponse(sessionId, TokenTypes.Bearer, Some(3600), None)

  /** Respond to the client with an unauthorized error and basic authentication required response header.
    */
  private def unauthorized(e: AuthError): ErrorResponse =
    val realm  = Current.getDomainDTO.domainId
    val suffix = e.error.fold("")(s => s""", error="$s"""")
    ErrorResponse.unauthorized.copy(headers = Map("WWW-Authenticate" -> s"""Basic realm="$realm"$suffix"""))
end OAuthTokenServlet

object OAuthTokenServlet:
  final val Url = "/oauth2/token"
