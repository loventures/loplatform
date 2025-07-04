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

package loi.cp.multidomain

import java.io.{PrintWriter, StringWriter}

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentSupport}
import com.learningobjects.cpxp.controller.upload.{UploadInfo, Uploader}
import com.learningobjects.cpxp.service.domain.{DomainState, DomainWebService}
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.util.logging.{LogCapture, ServletThreadLogWriter}
import com.learningobjects.cpxp.util.{EntityContext, HttpUtils, MimeUtils}
import com.learningobjects.de.authorization.Secured
import jakarta.servlet.http.HttpServletResponse
import loi.cp.admin.right.HostingAdminRight
import loi.cp.deploy.StylingServlet
import loi.cp.language.LanguageRootComponent

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

@Component
@Controller(value = "multi-domain", root = true)
@RequestMapping(path = "multiDomain")
@Secured(Array(classOf[HostingAdminRight])) // this is also overlord only
class MultiDomainRoot(val componentInstance: ComponentInstance)(
  dws: DomainWebService,
  ows: OverlordWebService,
) extends ApiRootComponent
    with ComponentImplementation:
  import MultiDomainRoot.*

  @RequestMapping(path = "languages/upload", method = Method.POST)
  def uploadOrReplaceNamedLanguagePack(webRequest: WebRequest): Seq[DomainResult] =
    val uploader: Uploader = Uploader.parse(webRequest.getRawRequest)
    val name               = uploader.getParameter("name")
    val language           = uploader.getParameter("language")
    val country            = Option(uploader.getParameter("country"))
    val upload             = uploader.getUpload("")
    forallDomains {
      val langRoot = ComponentSupport.get(classOf[LanguageRootComponent])
      langRoot.installOrReplace(name, language, country, upload)
      "OK"
    }
  end uploadOrReplaceNamedLanguagePack

  @RequestMapping(path = "sys/styling/{type}", method = Method.POST)
  def applyStyling(
    @PathVariable("type") uploadType: String,
    @RequestBody upload: UploadInfo,
    response: HttpServletResponse
  ): WebResponse =
    hijackLoggingAsResponse(response) {
      forallDomains {
        val ss = ComponentSupport.get(classOf[StylingServlet])
        ss.installFile(uploadType, upload.getFileName, upload.getFile, new PrintWriter(new StringWriter()))
        "OK"
      }
    }

  def hijackLoggingAsResponse[U](response: HttpServletResponse)(f: => U): WebResponse =
    HttpUtils.setExpired(response)
    response.setContentType(s"${MimeUtils.MIME_TYPE_TEXT_PLAIN}${MimeUtils.CHARSET_SUFFIX_UTF_8}")
    response.setStatus(HttpServletResponse.SC_OK)
    val writer = response.getWriter
    writer.println("Start")
    LogCapture.captureLogs(new ServletThreadLogWriter(response, writer, classOf[MultiDomainRoot])) {
      try f
      catch
        case NonFatal(e) =>
          logger.warn(e)("Could not perform multi-domain operation")
    }
    writer.println("End")
    writer.close()
    NoResponse
  end hijackLoggingAsResponse

  def forallDomains[T](f: => T): Seq[DomainResult] =
    ows.getAllDomains.asScala.toSeq filter { domain =>
      // It would be better if maintenance was not a database state but a persistent cluster property.
      // It would, however, need to survive a rolling restart.
      domain.getState == DomainState.Normal ||
      domain.getState == DomainState.Maintenance
    } map { domain =>
      logger.info(s"Processing domain ${domain.getDomainId}")
      EntityContext.flushClearAndCommit()
      val result = DomainResult(domain.getDomainId, domain.getName, domain.getPrimaryHostName, success = false, None)
      try
        dws.setupContext(domain.getId)
        val json = JacksonUtils.getMapper.valueToTree[JsonNode](f)
        result.copy(success = true, result = Some(json))
      catch
        case NonFatal(e) =>
          logger.warn(e)("Error processing domain")
          result
    }
end MultiDomainRoot

object MultiDomainRoot:
  private val logger = org.log4s.getLogger

case class DomainResult(
  domainId: String,
  domainName: String,
  hostName: String,
  success: Boolean,
  result: Option[JsonNode]
)
