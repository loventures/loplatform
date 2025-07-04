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

package loi.cp.filter

import com.google.common.net.HttpHeaders
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.Method.*
import com.learningobjects.cpxp.component.web.{FilterBinding, FilterComponent, FilterInvocation}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.service.facade.FacadeService
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.config.ConfigurationService
import loi.cp.security.SecuritySettings
import loi.cp.security.SecuritySettings.AllowedOrigins
import scaloi.syntax.BooleanOps.*

@Component
@FilterBinding(priority = 700)
class CorsHttpApiFilter(
  val componentInstance: ComponentInstance,
  sm: ServiceMeta
)(implicit fs: FacadeService, cs: ConfigurationService)
    extends FilterComponent
    with ComponentImplementation:
  import CorsHttpApiFilter.*

  override def filter(
    request: HttpServletRequest,
    response: HttpServletResponse,
    invocation: FilterInvocation
  ): Boolean =
    request.header(HttpHeaders.ORIGIN).fold(true)(processCorsOrigin(request, response))

  /** Process a CORS origin header and return whether to continue processing. */
  private def processCorsOrigin(request: HttpServletRequest, response: HttpServletResponse)(origin: String): Boolean =
    !(originMatch(origin) && applyHeaders(request, response, origin))

  /** Applies CORS header to the request and returns whether processing should halt. */
  private def applyHeaders(request: HttpServletRequest, response: HttpServletResponse, origin: String): Boolean =
    response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
    response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, AllowedMethods.mkString(","))
    response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
    response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, AllowedHeaders.mkString(","))

    OPTIONS.matches(request.getMethod) <|? {
      // If it's an OPTIONS request than return header and halt processing
      response.addHeader(HttpHeaders.ALLOW, AllowedMethods.mkString(","))
    }
  end applyHeaders

  /** Returns whether the request origin matches either the cluster CDN or one of the admin-configured allowed origins.
    */
  private def originMatch(origin: String): Boolean =
    // if origin, https://foo.cloudfront, contains static host, bar.cloudfront
    Option(sm.getStaticHost).exists(origin.contains) ||
      // or if explicitly allowed
      allowed.origins.contains(origin)

  private def allowed: AllowedOrigins =
    AllowedOrigins(SecuritySettings.config.getDomain.allowedOrigins)
end CorsHttpApiFilter

object CorsHttpApiFilter:
  private final val AllowedMethods =
    Seq(GET, POST, PUT, DELETE, PATCH, OPTIONS)

  private final val AllowedHeaders =
    Seq(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION)
