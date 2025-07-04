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
import com.learningobjects.cpxp.component.web.{FilterBinding, FilterComponent, FilterInvocation}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.util.{CdnUtils, EntityContext, HttpUtils}
import com.learningobjects.cpxp.{BaseServiceMeta, ServiceMeta, WebContext}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** A filter that processes requests from the CDN, transforming them into effective requests against individual domains.
  *
  * This would be more elegantly expressed as a series of functions, Request -> Response \/ Request'. However, until
  * then this will suffice.
  *
  * https://cluster/static/cdn/<host>/<request> gets interpreted as https://<expanded-host>/static/<request> with some
  * understanding of web resource versioning.
  *
  * https://cluster/cdn/<host>/<request> gets interpreted as https://<expanded-host>/<request>.
  */
@Component
@FilterBinding(priority = 100, system = true)
class CdnFilter(
  val componentInstance: ComponentInstance,
  serviceMeta: ServiceMeta,
  entityContext: => EntityContext,
  webContext: => WebContext
) extends FilterComponent
    with ComponentImplementation:
  import CdnFilter.*

  /** Filter an incoming request.
    * @param request
    *   the HTTP request
    * @param response
    *   the HTTP response
    * @param invocation
    *   the filter invocation context
    * @return
    *   true if the request was not a CDN request, otherwise false
    */
  override def filter(
    request: HttpServletRequest,
    response: HttpServletResponse,
    invocation: FilterInvocation
  ): Boolean =
    cdnRequestType(request).fold(true) { requestType => // it is a CDN request so process it
      handleCdnRequest(requestType)(using request, response, invocation)
      false
    }

  /** Parse a request to determine if it is a CDN request.
    * @param request
    *   the HTTP request
    * @return
    *   the CDN request if it is a CDN request
    */
  private def cdnRequestType(request: HttpServletRequest): Option[CdnRequestType] = request match
    case StaticCdnRequest(host, resource)  =>
      Some(staticCdnRequest(host, resource))
    case DynamicCdnRequest(host, resource) =>
      Some(dynamicCdnRequest(host, resource))
    case _                                 => None

  /** Parse a static CDN request.
    * @param host
    *   the host part from the request
    * @param resource
    *   the resource part from the request
    * @return
    *   the resulting request
    */
  private def staticCdnRequest(host: String, resource: String): CdnRequestType =
    val fqn      = fullyQualified(host)
    val uri      = s"/static$resource"
    val pathInfo = URLDecoder.decode(resource, StandardCharsets.UTF_8.name)
    if pathInfo.contains(CdnUtils.cdnSuffix(using serviceMeta, entityContext)) then EffectiveRequest(fqn, uri, pathInfo)
    else BrowserRedirect(fqn, uri)

  /** Parse a dynamic CDN request.
    * @param host
    *   the host part from the request
    * @param resource
    *   the resource part from the request
    * @return
    *   the resulting request
    */
  private def dynamicCdnRequest(host: String, resource: String): CdnRequestType =
    val fqn      = fullyQualified(host)
    val pathInfo = URLDecoder.decode(resource, StandardCharsets.UTF_8.name)
    EffectiveRequest(fqn, resource, pathInfo)

  /** Handle a CDN request.
    *
    * @param cdnRequest
    *   the CDN request type
    * @param request
    *   the HTTP request
    * @param response
    *   the HTTP response
    * @param invocation
    *   the filter invocation context
    */
  private def handleCdnRequest(
    cdnRequest: CdnRequestType
  )(implicit request: HttpServletRequest, response: HttpServletResponse, invocation: FilterInvocation): Unit =
    cdnRequest match
      case EffectiveRequest(host, uri, pathInfo) =>
        handleEffectiveRequest(new CdnServletRequest(request, host, uri, pathInfo))

      case BrowserRedirect(host, uri) =>
        handleBrowserRedirect(HttpUtils.getDomainHttpsUrl(host, request, uri))

  /** If the CDN is asking for a file from a different version of software than is running on this appserver, then
    * browser redirect so the user will get the file from their own appserver... This to handle a rolling upgrade where
    * the user is on app1 on v3 and requests a v3 resource from the CDN which in turn asks for the resource from an
    * appserver in the elb still running v2.
    */
  private def handleBrowserRedirect(redirect: String)(implicit response: HttpServletResponse): Unit =
    logger.info(s"CDN redirect: $redirect")
    HttpUtils.setExpired(response)
    response.sendRedirect(redirect)

  /** Continue processing a request from the CDN, reinterpreting the hostname and request URI as if it were a direct
    * browser request for the effective resource.
    *
    * @param cdnRequest
    *   the request
    * @param response
    *   the servlet response
    * @param invocation
    *   the filter invocation context
    */
  private def handleEffectiveRequest(
    cdnRequest: CdnServletRequest
  )(implicit response: HttpServletResponse, invocation: FilterInvocation): Unit =
    logger.info(s"CDN effective request: ${cdnRequest.getServerName}, ${cdnRequest.getRequestURI}")
    // Simulate a direct request to the relevant resource
    cdnRequest.setAttribute(CdnUtils.CdnRequestAttribute, true)
    // Update thread-local request storage. This would not be needed if this filter
    // was part of a series of monadic operations of the incoming request.
    webContext.init(cdnRequest, response)
    // Proceed with new simulated request
    invocation.proceed(cdnRequest, response)
  end handleEffectiveRequest
end CdnFilter

object CdnFilter:
  val logger = org.log4s.getLogger

  /** Pattern match for static resources: /static/cdn/<host>/<resource> */
  val StaticCdnRequest = HttpServletRequestMatcher("^/static/cdn/([^/]*)(/.*)")

  /** Pattern match for dynamic resources: /cdn/<host>/<resource> */
  val DynamicCdnRequest = HttpServletRequestMatcher("^/cdn/([^/]*)(/.*)")

  /** Return the fully qualified hostname from the host part embedded in a cdn request.
    *
    * @param host
    *   the host part from the cdn request
    * @return
    *   the supplied host, if fully qualified, otherwise that host with the default domain suffix appended
    */
  private def fullyQualified(host: String) =
    if host.contains('.') then host
    else host.concat(BaseServiceMeta.getServiceMeta.getStaticSuffix)
end CdnFilter
