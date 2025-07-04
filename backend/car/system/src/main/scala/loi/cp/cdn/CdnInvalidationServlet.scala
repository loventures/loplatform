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

package loi.cp.cdn

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{AbstractComponentServlet, ServletBinding}
import com.learningobjects.cpxp.scala.util.HttpSessionOps.*
import com.learningobjects.cpxp.util.{CdnUtils, SessionUtils}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

/** Servlet for forcing CDN reload. When POSTed to, causes all clients to request a non-cached version of CDN assets,
  * which will cause CloudFront to reload the data directly from the server.
  *
  * Since this is often a hosting concern, it also accepts requests from the app server's localhost.
  */
@Component
@ServletBinding(path = "/sys/cdn/antivenin", system = true)
class CdnInvalidationServlet extends AbstractComponentServlet:
  import CdnInvalidationServlet.*

  override def get(req: HttpServletRequest, rsp: HttpServletResponse): Unit =
    rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)

  override def post(req: HttpServletRequest, rsp: HttpServletResponse): Unit =
    if isOverlord(req) || isLocal(req) then CdnUtils.incrementCdnVersion()
    else rsp.sendError(HttpServletResponse.SC_FORBIDDEN)
end CdnInvalidationServlet

object CdnInvalidationServlet:
  private def isOverlord(req: HttpServletRequest): Boolean =
    Option(req.getSession(false)).flatMap(_.attr[String](SessionUtils.SESSION_ATTRIBUTE_OVERLORD)).contains("true")

  private def isLocal(req: HttpServletRequest): Boolean =
    localhosts contains req.getRemoteAddr

  private final val localhosts = Set("::1", "0:0:0:0:0:0:0:1", "127.0.0.1")
