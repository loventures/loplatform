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

package loi.cp.password

import com.learningobjects.cpxp.component.annotation.{Controller, QueryParam, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, HttpContext, Method}
import com.learningobjects.cpxp.service.token.TokenType
import com.learningobjects.de.authorization.Secured
import loi.cp.web.challenge.ChallengeGuard
import scalaz.\/

import scala.concurrent.Future

@Controller(value = "recoverPasswords", root = true)
trait RecoverPasswordRootApi extends ApiRootComponent:
  import RecoverPasswordRootApi.*

  /** Send a password recovery email to the located account.
    *
    * @param search
    *   The value to search for.
    * @param properties
    *   What properties to search (userName, emailAddress).
    * @param redirect
    *   What redirect URL to include in the recovery email. Should start with /. A token (e.g. abc-1234) will be
    *   appended to this URL; the URL should end with a query string or fragment to accommodate it.
    */
  @RequestMapping(path = "passwords/recover", method = Method.POST)
  @Secured(guard = Array(classOf[ChallengeGuard]), allowAnonymous = true)
  def recoverPassword(
    @QueryParam("search") search: String,
    @QueryParam("properties") properties: Seq[String],
    @QueryParam("redirect") redirect: String
  ): Future[PasswordError \/ Unit]

  @RequestMapping(path = "passwords/reset", method = Method.GET)
  @Secured(allowAnonymous = true)
  def validateToken(@QueryParam("token") token: String): PasswordError \/ ResetValidity

  @RequestMapping(path = "passwords/reset", method = Method.POST)
  @Secured(allowAnonymous = true)
  def resetPassword(
    @QueryParam("token") token: String,
    @QueryParam("password") password: String,
    context: HttpContext
  ): PasswordError \/ Unit

  @RequestMapping(path = "passwords/reject", method = Method.POST)
  @Secured(allowAnonymous = true)
  def rejectAccount(@QueryParam("token") token: String): PasswordError \/ Unit
end RecoverPasswordRootApi

object RecoverPasswordRootApi:
  case class ResetValidity(userName: String, `type`: TokenType)
