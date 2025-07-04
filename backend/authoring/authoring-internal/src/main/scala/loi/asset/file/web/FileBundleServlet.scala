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

package loi.asset.file.web

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.web.util.UriTemplate
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.asset.file.fileBundle.FileBundleService
import loi.asset.file.fileBundle.model.FileBundle
import loi.authoring.asset.service.exception.NoSuchAssetException
import loi.authoring.attachment.service.exception.AssetHasNoAttachment
import loi.authoring.blob.exception.NoSuchBlobRef
import loi.authoring.node.AssetNodeService
import scalaz.syntax.std.boolean.*
import scaloi.syntax.OptionOps.*

import java.net.URLDecoder
import java.nio.charset.Charset
import scala.jdk.CollectionConverters.*
import scala.util.Try

/** This servlet exists to get around the fact that SRS chains its processing of the URI
  *
  * For file bundles, we actually want to serve a specific file out of a ZIP and that while might be several directories
  * deep, i.e. "/foo/bar/image.jpg" This servlet parses the id of the asset and uses the path parameter to serve a file
  * out
  */
@Component
@ServletBinding(path = "/api/v2/assets/fileBundle.1")
class FileBundleServlet(
  fileBundleService: FileBundleService,
  nodeService: AssetNodeService
) extends AbstractComponentServlet:

  override def service(request: HttpServletRequest, response: HttpServletResponse): WebResponse =
    request.getRequestURI match
      case Paths.serve(id) =>
        val pathInZip   = Try(request.getRequestURI.split(id + "/")(1))
          .map(path => URLDecoder.decode(path, Charset.forName("UTF-8")))
          .getOrElse("/index.html")
        val loadAttempt = for
          asset0   <- nodeService.loadRawByGuessing(id.toLong).toTry(NoSuchAssetException(id.toLong))
          asset    <- asset0.filter[FileBundle].toTry(NoSuchAssetException(id.toLong))
          fileInfo <- fileBundleService.fileInfo(asset, pathInZip)
        yield
          if !fileInfo.exists() then ErrorResponse.notFound
          else FileResponse(fileInfo)

        loadAttempt
          .recover({
            case ex: NoSuchAssetException => ErrorResponse.notFound(ex.getErrorMessage)
            case ex: AssetHasNoAttachment => ErrorResponse.notFound(ex.getErrorMessage)
            case ex: NoSuchBlobRef        => ErrorResponse.notFound(ex.getErrorMessage)
          })
          .get

      case _ => ErrorResponse.notFound
end FileBundleServlet

object Paths:

  // Code is taken from BadgingServlet
  sealed class ReqPattern(pattern: String):

    val templ = new UriTemplate(pattern) // Stolen from SRS

    def unapplySeq(uri: String): Option[Seq[String]] =
      templ.matches(uri).option(templ.getVariableNames.asScala.toSeq.map(templ.`match`(uri).asScala))

    def apply(vals: Any*): String =
      templ.createURI(vals map (_.toString)*)

  val serve = new ReqPattern("/api/v2/assets/fileBundle.1/{id}")
end Paths
