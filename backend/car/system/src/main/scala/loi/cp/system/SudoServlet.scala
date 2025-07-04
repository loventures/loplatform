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

import com.learningobjects.cpxp.component.ComponentSupport
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{AbstractComponentServlet, ServletBinding}
import com.learningobjects.cpxp.controller.login.LoginController
import com.learningobjects.cpxp.service.accesscontrol.AccessControlException
import com.learningobjects.cpxp.service.user.UserWebService
import com.learningobjects.cpxp.util.HttpUtils
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.admin.right.user.SudoAdminRight
import loi.cp.right.RightService

import java.io.FileNotFoundException

/** This endpoint is only provided as a convenience for developers/testers. It allows privileged users to log in as
  * other users. Navigate to /control/sudo/bdobbs to log in as bdobbs. Navigate to /control/exit to log back.
  */
@Component
@ServletBinding(path = SudoServlet.ControlSudo)
class SudoServlet(rs: RightService, uws: UserWebService) extends AbstractComponentServlet:
  import SudoServlet.*

  override def get(request: HttpServletRequest, response: HttpServletResponse): Unit =
    HttpUtils.sendAutopost(response, HttpUtils.getUrl(request, request.getRequestURI))

  override def post(request: HttpServletRequest, response: HttpServletResponse): Unit =
    if !rs.getUserHasRight(classOf[SudoAdminRight]) then throw new AccessControlException("Insufficient rights")
    val username = request.getRequestURI.stripPrefix(ControlSudo).substring(1)
    val user     = Option(uws.getUserByUserName(username)) getOrElse {
      throw new FileNotFoundException(s"Unknown user: $username")
    }
    if !rs.isSuperiorToUser(user) then throw new AccessControlException(s"Privilege escalation: $username")
    logger.info(s"Sudo $username: ${user.getId}")
    ComponentSupport.newInstance(classOf[LoginController]).logInAs(user.getId, null)
    response.sendRedirect("/")
  end post
end SudoServlet

object SudoServlet:
  private final val logger = org.log4s.getLogger

  final val ControlSudo = "/control/sudo"
