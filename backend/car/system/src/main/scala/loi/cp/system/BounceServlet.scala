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
import com.learningobjects.cpxp.component.util.HtmlTemplate
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.Method.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.upgrade.UpgradeService
import jakarta.servlet.http.{Cookie, HttpServletRequest, HttpServletResponse}
import scalaz.syntax.either.*
import scaloi.syntax.AnyOps.*

import scala.jdk.CollectionConverters.*

/** Provides support for bouncing among nodes in a multinode system. To bounce between nodes this just clears the load
  * balancer cookies and hopes.
  */
@Component
@ServletBinding(path = BounceServlet.ControlBounce, system = true)
class BounceServlet(
  val componentInstance: ComponentInstance,
  serviceMeta: ServiceMeta,
  upgradeService: UpgradeService
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import BounceServlet.*
  import ServletDispatcher.*

  override def handler: RequestHandler = {
    case RequestMatcher(GET, ControlBounce, _, _) =>
      HtmlResponse(
        HtmlTemplate(this, "bounce.html").bind(
          "serviceMeta" -> serviceMeta,
          "hosts"       -> upgradeService.findRecentHosts.asScala.sortBy(_.getCentralHost)
        )
      ).right

    case RequestMatcher(POST, ControlBounce, request, response) =>
      CookieNames foreach { name =>
        response.addCookie(awselbCookie(request, name))
      }
      // Why? Because this forces a connection close (see Http11Processor#statusDropsConnection)
      // which may encourage the LB to issue a new cookie...
      ErrorResponse(HttpServletResponse.SC_NOT_IMPLEMENTED).left
  }

  /** A cookie value that clears the load balancer session-affinity cookie. */
  private def awselbCookie(request: HttpServletRequest, name: String): Cookie =
    new Cookie(name, "") <| { cookie =>
      cookie.setHttpOnly(true)
      cookie.setMaxAge(0)
      cookie.setPath("/")
      cookie.setSecure(request.isSecure)
    }
end BounceServlet

object BounceServlet:

  /** The URL this binds to. */
  final val ControlBounce = "/control/bounce"

  /** The names of the AWS ALB node affinity cookies. */
  final val CookieNames = "AWSALB" :: "AWSALBCORS" :: Nil
