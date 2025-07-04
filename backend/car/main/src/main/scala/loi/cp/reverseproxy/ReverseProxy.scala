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

package loi.cp.reverseproxy

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.site.ItemSiteComponent
import com.learningobjects.cpxp.component.web.{Method, NoResponse, RedirectResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.name.NameService
import com.learningobjects.cpxp.service.site.SiteFacade
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

import java.lang.Long as JLong
import java.net.URI
import scala.util.Try
import scala.util.matching.Regex

@Component(alias = Array("loi.cp.reverseproxy.ReverseProxySite"))
class ReverseProxy(
  val componentInstance: ComponentInstance,
  self: SiteFacade,
  ns: NameService,
  req: HttpServletRequest,  // yuck
  rsp: HttpServletResponse, // yuck
  pool: ReverseProxyPool,
  implicit val fs: FacadeService
) extends ReverseProxyComponent
    with ItemSiteComponent
    with ComponentImplementation:

  @PostCreate
  private def create(proxy: ReverseProxyComponent): Unit =
    update(proxy)

  override def update(proxy: ReverseProxyComponent): ReverseProxyComponent =
    validateSettings(proxy)
    self.setName(proxy.getName)
    self.setUrl(proxy.getUrl)
    val configuration = ReverseProxyConfiguration(proxy.getRemoteUrl, proxy.getCookieNames, proxy.getRewriteRules)
    self.setJson(configuration)
    self.setDisabled(proxy.isDisabled)
    this

  override def delete(): Unit = self.delete()

  override def getId: JLong = componentInstance.getId

  override def getName: String = self.getName

  override def getUrl: String = self.getUrl

  override def getRemoteUrl: String = getConfiguration.remoteUrl

  override def getCookieNames: List[String] = getConfiguration.cookieNames

  override def getRewriteRules: List[RewriteRule] =
    getConfiguration.rewriteRules

  override def isDisabled: Boolean = self.getDisabled.booleanValue

  override def renderSite(view: String): WebResponse =
    if req.getRequestURI
        .equals(self.getUrl) && (self.getJson(classOf[ReverseProxyConfiguration]).remoteUrl.indexOf('/', 8) < 0)
    then
      // Handle degenerate case /foo -> http://example.org -> /foo/
      // in all other cases, the request is proxied directly.
      return RedirectResponse.permanent(self.getUrl + "/")
    pool.doProxy(self, req, rsp)
    NoResponse // YUCK, doProxy should return Renderable although async is hard

  @Rpc(method = Method.GET) @Direct // do we need this?
  def notFound(): WebResponse =
    pool.doProxy(self, req, rsp)
    NoResponse

  private def getConfiguration: ReverseProxyConfiguration =
    self.getJson(classOf[ReverseProxyConfiguration])

  private val UrlRe = "^(?:/[a-zA-Z0-9_]+)+$".r

  private def validateSettings(proxy: ReverseProxyComponent): Unit =
    if proxy.getName.isEmpty then throw new ValidationException("name", proxy.getName, "Name is required")
    if UrlRe.findFirstIn(proxy.getUrl).isEmpty then throw new ValidationException("url", proxy.getUrl, "Invalid URL")
    if (proxy.getUrl != getUrl) && Option(ns.getItemId(proxy.getUrl)).isDefined then
      // TODO: Component / Web servlet lookup
      throw new ValidationException("url", proxy.getUrl, "URL is already bound")
    if !proxy.getRemoteUrl.startsWith("http") || proxy.getRemoteUrl.endsWith("/") || proxy.getRemoteUrl.contains(
        Current.getDomainDTO.hostName
      ) || Try(new URI(proxy.getRemoteUrl)).isFailure
    then throw new ValidationException("remoteUrl", proxy.getRemoteUrl, "Invalid remote URL")
  end validateSettings
end ReverseProxy

case class ReverseProxyConfiguration(
  remoteUrl: String,
  cookieNames: List[String],
  rewriteRules: List[RewriteRule]
)

case class CompiledRewrite(bodyRegex: Regex, replacementText: String)

object CompiledRewrite:
  def apply(rule: RewriteRule): CompiledRewrite =
    new CompiledRewrite(rule.bodyPattern.r, rule.replacementText)
