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

package loi.cp.status

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.Method.GET
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFacade, DomainWebService, SecurityLevel}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.util.HttpUtils
import jakarta.servlet.http.HttpServletRequest
import scalaz.std.string.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scaloi.syntax.OptionOps.*

/** Provides support for viewing cluster status.
  */
@Component
@ServletBinding(path = ClusterStatusServlet.ControlClusterStatus, system = true)
class ClusterStatusServlet(val componentInstance: ComponentInstance)(implicit
  dws: DomainWebService,
  fs: FacadeService,
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import ClusterStatusServlet.*
  import HttpUtils.*
  import ServletDispatcher.*

  override def handler: RequestHandler = { case RequestMatcher(GET, ControlClusterStatus, req, _) =>
    (
      if !req.isSecure && domain(req).exists(_.securityLevel ne SecurityLevel.NoSecurity) then
        // kick us to https otherwise APIs return confusion
        val origPath = req.getRequestURI + OptionNZ(req.getQueryString).map("?" + _).orZero
        RedirectResponse.temporary(getHttpsUrl(req, origPath))
      else HtmlResponse(this, "clusterStatus.html")
    )
    .right
  }

  private def domain(req: HttpServletRequest) =
    dws
      .getDomainIdByHost(req.getServerName)
      .facade_?[DomainFacade]
      .map(DomainDTO(_))
end ClusterStatusServlet

object ClusterStatusServlet:

  /** The URL this binds to. */
  final val ControlClusterStatus = "/control/clusterStatus"
