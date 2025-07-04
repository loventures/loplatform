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

package loi.asset.fileBundle
package web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import loi.asset.file.fileBundle.FileBundleService
import loi.asset.file.fileBundle.model.{DisplayFile, FileBundle}
import loi.authoring.attachment.service.exception.{AssetHasNoAttachment, ZipAttachmentFileNotFound}
import loi.authoring.blob.exception.NoSuchBlobRef
import loi.authoring.node.AssetNodeService
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException
import scalaz.std.string.*
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.OptionOps.*
import scaloi.syntax.TryOps.*

import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.UUID
import scala.util.Try

@Component
@Controller(value = "fileBundle.1", root = true)
class FileBundleWebController(val componentInstance: ComponentInstance)(
  fileBundleService: FileBundleService,
  nodeService: AssetNodeService,
  webUtils: AuthoringWebUtils,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "authoring/{commit}/fileBundle.1/{name}/files", method = Method.GET)
  def getDisplayFiles(
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") nodeName: UUID,
  ): Try[List[DisplayFile]] =

    val ws = webUtils.detachedWorkspaceOrThrow404(commitId)

    val attempt =
      for node <- nodeService.loadA[FileBundle](ws).byName(nodeName)
      yield node.data.displayFiles.toList

    attempt.mapExceptions { case ex: NoSuchNodeInWorkspaceException =>
      notFound(ex)
    }
  end getDisplayFiles

  @RequestMapping(
    path = "authoring/{commit}/fileBundle.1/{name}/serve",
    method = Method.GET,
    terminal = true,
  )
  def serveFileBundle(
    webRequest: WebRequest,
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") nodeName: UUID,
  ): Try[WebResponse] =
    val path = webRequest.getRawRequest.getRequestURI
      .split("serve", 2)
      .lastOption
      .map(path => URLDecoder.decode(path, Charset.forName("UTF-8")))
      .filterNot(_ == "/")
      .filterNZ
      .getOrElse("index.html")

    val ws = webUtils.detachedWorkspaceOrThrow404(commitId)

    val attempt = for
      node     <- nodeService.loadA[FileBundle](ws).byName(nodeName)
      fileInfo <- fileBundleService.fileInfo(node, path)
      _        <- fileInfo.getFile.isFile.elseFailure(ZipAttachmentFileNotFound(node.info.id, path))
    yield FileResponse(fileInfo)

    attempt.mapExceptions {
      case ex: AssetHasNoAttachment           => notFound(ex)
      case ex: NoSuchBlobRef                  => notFound(ex)
      case ex: NoSuchNodeInWorkspaceException => notFound(ex)
      case ex: ZipAttachmentFileNotFound      => notFound(ex)
    }
  end serveFileBundle
end FileBundleWebController
