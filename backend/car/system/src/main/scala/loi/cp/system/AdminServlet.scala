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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.Method.{GET, POST}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.user.UserWebService
import com.learningobjects.cpxp.util.HttpUtils
import jakarta.servlet.http.HttpServletResponse
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scaloi.syntax.BooleanOps.*

import java.util.Date

@Component
@ServletBinding(path = AdminServlet.ControlAdmin, system = true)
class AdminServlet(
  val componentInstance: ComponentInstance,
  overlordWebService: OverlordWebService,
  domainWebService: DomainWebService,
  userWebService: UserWebService
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import AdminServlet.*
  import ServletDispatcher.*

  override def handler: RequestHandler = {
    case RequestMatcher(GET, ControlAdmin, request, _) =>
      autopost(HttpUtils.getHttpsUrl(request, AdminServlet.ControlAdmin)).right

    case RequestMatcher(POST, ControlAdmin, request, response) =>
      for
        _      <- request.isSecure \/> ErrorResponse.unauthorized
        domain <-
          Option(overlordWebService.findOverlordDomain) \/> ErrorResponse(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
      yield
        Current.setTime(new Date)
        Current.setDomainDTO(domainWebService.getDomainDTO(domain.getId))
        val id   = overlordWebService.getAnonymousUserId(domain.getId)
        val user = userWebService.getUserDTO(id)
        Current.setUserDTO(user)
        CurrentFilter.login(request, response, user, false) // magic to make the domain stick
        RedirectResponse.temporary("/")
  }
end AdminServlet

object AdminServlet:
  final val ControlAdmin = "/control/admin"
