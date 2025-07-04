package loi.cp.localProxy

import java.io.StringWriter
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.{BaseHtmlWriter, ComponentEnvironment, ComponentInterface}
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.template.{LohtmlTemplate, RenderContext}
import com.learningobjects.cpxp.component.web.{StreamResponse, TextResponse, WebResponse}
import com.learningobjects.cpxp.util.HttpUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse.SC_OK
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet

import java.net.URI
import java.nio.charset.StandardCharsets

@Service
class LocalProxy(environment: ComponentEnvironment):
  import LocalProxy.*

  def proxyRequest(
    context: ComponentInterface,
    base: String,
    request: HttpServletRequest,
    pathInfo: Option[String],
  ): Option[WebResponse] =
    val identifier      = context.getComponentInstance.getIdentifier
    val componentConfig = environment.getJsonConfiguration(identifier)
    val enabled         = componentConfig.get("localProxyEnabled")
    val url             = componentConfig.get("localProxyUrl")
    PartialFunction.condOpt(enabled -> url):
      case (java.lang.Boolean.TRUE, url: String) =>
        val path = request.getRequestURI.stripPrefix(base)
        val pi   = pathInfo.getOrElse(if ViteRE.matches(path) then path else "/index.html") // SPA
        val qs   = Option(request.getQueryString).fold("")(_.prepended('?'))
        val uri  = URI.create(url).resolve(pi + qs)
        val get  = new HttpGet(uri)
        val rsp  = HttpUtils.getHttpClient.execute(get)
        if RewriteRE.matches(pi) then
          val content   = IOUtils.toString(rsp.getEntity.getContent, StandardCharsets.UTF_8)
          // First we replace all the absolute hrefs in the returned content with /Authoring-prefixed hrefs
          // Then we hack the @vite/client script to think the URL base is /Authoring/. We can't edit base
          // in the config because that affects client and server.
          val replaced  = content
            .replaceAll(
              "((?:import \"|from \"|src=\"|url\\((?:'|\\\\\")?))(/(?:src|node_modules|@react-refresh|@vite))",
              s"$$1$base$$2"
            )
            .replaceAll(
              "\"(/src/(?:scripts|i18n))",
              s"\"$base$$1"
            )
            .replace("const base = \"/\"", s"const base =\"$base/\"")
          val rendered  = if RenderRE.matches(pi) then
            val scope    = context.getComponentInstance.getComponent
            val strings  = new StringWriter()
            val out      = new BaseHtmlWriter(strings)
            val renderer = new RenderContext(scope, context, out, null, null)
            LohtmlTemplate.forTemplate(replaced).render(renderer)
            strings.toString
          else replaced
          val mediaType =
            MediaType.parse(rsp.getEntity.getContentType.getValue).withCharset(StandardCharsets.UTF_8)
          TextResponse.of(rendered, mediaType)
        else
          StreamResponse(rsp.getEntity.getContent, SC_OK, Map("Content-Type" -> rsp.getEntity.getContentType.getValue))

object LocalProxy:
  val RewriteRE =
    "/|/(?:index|instructor)\\.html|.*(\\.css|\\.sass|\\.scss|\\.js|\\.jsx|\\.ts|\\.tsx|\\.mjs|@vite/client|@react-refresh)".r

  val RenderRE =
    "/|/(?:index|instructor)\\.html".r

  val ViteRE = "/(?:src|node_modules|@vite)/.*|/@react-refresh".r
