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

package loi.cp.exeunt

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentSupport}
import com.learningobjects.cpxp.controller.login.LoginController
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.scala.util.HttpSessionOps.*
import com.learningobjects.cpxp.util.SessionUtils
import scalaz.syntax.std.boolean.*
import scaloi.misc.Extractor

/** Slash sys slash go home. Support for logging back in as whom you were and going back whence you came.
  */
@Component
@ServletBinding(path = EuntDomusServlet.Path)
class EuntDomusServlet(val componentInstance: ComponentInstance)
    extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:

  import EuntDomusServlet.*
  import ServletDispatcher.*

  protected def handler: RequestHandler = { case RequestMatcher(Method.GET, PathInfo(pi), req, _) =>
    for
      sudoer <- req.getSession.attr[String](SessionUtils.SESSION_ATTRIBUTE_SUDOER).map(_.replaceAll(".*:", ""))
      target <- req.param("user")
      if sudoer == target
    do ComponentSupport.newInstance(classOf[LoginController]).logBack()
    ReturnUrlRe.matches(pi) either RedirectResponse.temporary(pi) or ErrorResponse.badRequest(pi)
  }
end EuntDomusServlet

object EuntDomusServlet:
  final val Path = "/sys/eunt/domus"

  private val PathInfo = Extractor `dropPrefix` Path

  private final val ReturnUrlRe = "/Administration/((Course|Test)Sections/\\d+/Enrollments|Users)|/Domains|/Users".r
