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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method, RedirectResponse, WebResponse}
import com.learningobjects.de.authorization.Secured
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import scalaz.\/

import java.lang.{Boolean as JBoolean, Long as JLong}

/** Session root. CSRF checking is disabled on these routes to allow login to occur even when the front end is bereft of
  * a valid CSRF token.
  */
@Controller(value = "sessions", root = true)
@RequestMapping(path = "sessions")
trait SessionRootComponent extends ApiRootComponent:
  @RequestMapping(path = "get", method = Method.GET)
  @Secured(allowAnonymous = true)
  def session: Option[SessionComponent]

  @RequestMapping(path = "login", method = Method.POST, csrf = false)
  @Secured(allowAnonymous = true)
  def login(
    @QueryParam username: String,
    @QueryParam password: String,
    @QueryParam(required = false) remember: JBoolean, // decoding Option[Boolean] is hard
    @QueryParam(required = false) mechanism: JLong,   // decoding Option[Long] is hard
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): LoginError \/ Login

  @RequestMapping(path = "loginRedirect", method = Method.POST, csrf = false)
  @Secured(allowAnonymous = true)
  def loginRedirect(
    @QueryParam username: String,
    @QueryParam(required = false) path: String,
    request: HttpServletRequest,
  ): ErrorResponse \/ RedirectResponse

  @RequestMapping(path = "logout", method = Method.POST, csrf = false)
  @Secured(allowAnonymous = true)
  def logout(request: HttpServletRequest, response: HttpServletResponse): String

  @RequestMapping(path = "logout", method = Method.DELETE, csrf = false)
  @Secured(allowAnonymous = true)
  def clearLoggedOut(request: HttpServletRequest, response: HttpServletResponse): Unit

  @RequestMapping(path = "exit", method = Method.POST, csrf = false)
  @Secured(allowAnonymous = true)
  def exit(request: HttpServletRequest, response: HttpServletResponse): String

  @RequestMapping(path = "resurrect", method = Method.POST, csrf = false)
  @Secured(allowAnonymous = true)
  def resurrect(
    @QueryParam("sessionId") sessionId: String,
    @QueryParam("redirectUrl") redirectUrl: String,
    @QueryParam("resurrectToken") resurrectToken: String,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): WebResponse

  /** Authenticates without logging in the current user. Used to verify password. */
  def authenticate(username: String, password: String, mechanism: JLong): LoginError \/ Login
end SessionRootComponent
