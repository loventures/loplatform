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

package loi.cp.maintenance

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{AbstractComponentServlet, ServletBinding}
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.util.HttpUtils
import jakarta.servlet.http.{Cookie, HttpServletRequest, HttpServletResponse}
import scaloi.syntax.AnyOps.*

import scala.concurrent.duration.*

/** Sets a 1 hour maintenance bypass cookie. Used to allow QA of a system while it is in maintenance mode.
  */
@Component
@ServletBinding(path = MaintenanceBypassServlet.ControlMaintenanceBypass, system = true, transact = false)
class MaintenanceBypassServlet extends AbstractComponentServlet:

  /** Autopost the user to the same URL, defeat browser pre-fetch. */
  override def get(request: HttpServletRequest, response: HttpServletResponse): Unit =
    HttpUtils.sendAutopost(response, MaintenanceBypassServlet.ControlMaintenanceBypass)

  /** Set maintenance bypass cookie and redirect to /. */
  override def post(request: HttpServletRequest, response: HttpServletResponse): Unit =
    response.addCookie(maintenanceBypassCookie)
    response.sendRedirect("/")

  /** Construct a one hour maintenance bypass cookie. */
  private def maintenanceBypassCookie: Cookie =
    new Cookie(CurrentFilter.HTTP_COOKIE_MAINTENANCE_BYPASS, "true") <| { cookie =>
      cookie.setSecure(false)
      cookie.setHttpOnly(true)
      cookie.setMaxAge(1.hour.toSeconds.toInt)
      cookie.setPath("/")
    }
end MaintenanceBypassServlet

object MaintenanceBypassServlet:
  final val ControlMaintenanceBypass = "/control/maintenance/bypass"
