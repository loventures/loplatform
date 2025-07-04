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

package loi.cp.system

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.Method.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.util.HttpUtils
import scalaz.syntax.either.*
import scaloi.syntax.boolean.*

@Component
@ServletBinding(path = LoginServlet.ControlLogin)
class LoginServlet(val componentInstance: ComponentInstance)(
  domain: DomainDTO,
  sm: ServiceMeta,
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import LoginServlet.*
  import ServletDispatcher.*

  override def handler: RequestHandler =
    case RequestMatcher(GET, ControlLogin, request, _) =>
      autopost(HttpUtils.getUrl(request, ControlLogin, domain.securityLevel.getIsSecure)).right

    case RequestMatcher(POST, ControlLogin, request, response) =>
      for _ <- (request.isSecure || !sm.isProdLike) \/> ErrorResponse.unauthorized
      yield
        CurrentFilter.logout(request, response)
        HtmlResponse(this, "login.html")

end LoginServlet

object LoginServlet:
  final val ControlLogin = "/control/login"
