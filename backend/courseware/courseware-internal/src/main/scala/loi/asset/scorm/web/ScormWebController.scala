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

package loi.asset.scorm.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import loi.asset.file.fileBundle.FileBundleService
import loi.asset.html.model.Scorm
import loi.authoring.asset.Asset
import loi.authoring.attachment.service.exception.{AssetHasNoAttachment, ZipAttachmentFileNotFound}
import loi.authoring.blob.exception.NoSuchBlobRef
import loi.authoring.node.AssetNodeService
import loi.authoring.project.AccessRestriction
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.i18n.AuthoringBundle
import scalaz.std.string.*
import scalaz.syntax.std.option.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*

import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.UUID
import scala.util.Try

@Component
@Controller(value = "scorm.1", root = true)
class ScormWebController(val componentInstance: ComponentInstance)(
  fileBundleService: FileBundleService,
  nodeService: AssetNodeService,
  workspaceService: ReadWorkspaceService,
) extends ApiRootComponent
    with ComponentImplementation:

  // TODO: a more reasonable student facing request, but also we should do that for filebundle at the same time

  @RequestMapping(
    path = "authoring/branches/{branchId}/scorm.1/{name}/serve",
    method = Method.GET,
    terminal = true,
  )
  def serveBranchScormFile(
    webRequest: WebRequest,
    @PathVariable("branchId") branchId: Long,
    @PathVariable("name") nodeName: UUID,
  ): Try[WebResponse] =
    serveImpl(webRequest, Left(branchId), nodeName)

  @RequestMapping(
    path = "authoring/{commit}/scorm.1/{name}/serve",
    method = Method.GET,
    terminal = true,
  )
  def serveScormFile(
    webRequest: WebRequest,
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") nodeName: UUID,
  ): Try[WebResponse] =
    serveImpl(webRequest, Right(commitId), nodeName)

  def serveImpl(
    webRequest: WebRequest,
    branchOrCommit: Either[Long, Long],
    nodeName: UUID,
  ): Try[WebResponse] =
    val uriParts  = webRequest.getRawRequest.getRequestURI.split("serve", 2)
    // This is the path following the serve URL, i.e. asdf-edge-path/serve/<everything-over-here>
    // if not defined, we seek the manifest for a default
    val maybePath = uriParts.lastOption
      .map(path => URLDecoder.decode(path, Charset.forName("UTF-8")))
      .filterNot(_ == "/")
      .filterNZ

    maybePath
      .cata(
        path =>
          // if the path was a normal path, render the file
          val attempt = for
            node     <- loadScorm(branchOrCommit, nodeName)
            fileInfo <- fileBundleService.fileInfo(node, path.replaceFirst("\\?.*", "")) // get without ?param
            _        <- fileInfo.getFile.isFile.elseFailure(ZipAttachmentFileNotFound(node.info.id, path))
          yield FileResponse(fileInfo)

          attempt.mapExceptions {
            case ex: AssetHasNoAttachment           => notFound(ex)
            case ex: NoSuchBlobRef                  => notFound(ex)
            case ex: NoSuchElementException         => notFound(AuthoringBundle.noSuchBranch(branchOrCommit.merge))
            case ex: NoSuchNodeInWorkspaceException => notFound(ex)
            case ex: ZipAttachmentFileNotFound      => notFound(ex)
          }
        ,
        /** Else, if we have no path, redirect to the default resourcePath
          *
          * NOTE: FileBundleWebController simply returns the file directly in the default case, because it is rendering
          * index.html. SCORM however needs to redirect, because often the resource is not at the root, i.e.
          * 'shared/launchpage.html'. If this html page then references others css/js files, any relative paths will not
          * resolve correctly
          */
        for node <- loadScorm(branchOrCommit, nodeName)
        /**/
        yield RedirectResponse.permanent(uriParts(0) + "serve/" + node.data.resourcePath)
      )
  end serveImpl

  private def loadScorm(branchOrCommit: Either[Long, Long], nodeName: UUID): Try[Asset[Scorm]] =
    for
      ws   <- Try(
                branchOrCommit.fold(
                  workspaceService.requireReadWorkspace(_, AccessRestriction.none),
                  workspaceService.requireDetachedWorkspace
                )
              )
      node <- nodeService.loadA[Scorm](ws).byName(nodeName)
    yield node
end ScormWebController
