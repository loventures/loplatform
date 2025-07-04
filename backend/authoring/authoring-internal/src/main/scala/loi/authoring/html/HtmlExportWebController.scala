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

package loi.authoring.html

import com.google.common.net.MediaType as GediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import loi.asset.course.model.Course
import loi.asset.util.Assex.*
import loi.authoring.edge.EdgeService
import loi.authoring.node.AssetNodeService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.option.*

import java.util.UUID

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[html] class HtmlExportWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
  htmlExportService: HtmlExportService,
)(implicit
  nodeService: AssetNodeService,
  edgeService: EdgeService,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "authoring/{branch}/asset/{name}/export/html", method = Method.GET)
  def exportHtmlZip(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: UUID,
    request: WebRequest
  ): FileResponse[?] = exportHtml(branchId, None, name, request, doc = false)

  @RequestMapping(path = "authoring/{branch}/commits/{commit}/asset/{name}/export/html", method = Method.GET)
  def exportHtmlZip(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") name: UUID,
    request: WebRequest
  ): FileResponse[?] = exportHtml(branchId, Some(commitId), name, request, doc = false)

  @RequestMapping(path = "authoring/{branch}/asset/{name}/export/doc", method = Method.GET)
  def exportHtmlDoc(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: UUID,
    request: WebRequest
  ): FileResponse[?] = exportHtml(branchId, None, name, request, doc = true)

  @RequestMapping(path = "authoring/{branch}/commits/{commit}/asset/{name}/export/doc", method = Method.GET)
  def exportHtmlDoc(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") name: UUID,
    request: WebRequest
  ): FileResponse[?] = exportHtml(branchId, Some(commitId), name, request, doc = true)

  private def exportHtml(
    branchId: Long,
    commitOpt: Option[Long],
    name: UUID,
    request: WebRequest,
    doc: Boolean,
  ): FileResponse[?] =
    val workspace = webUtils.workspaceOptionallyAtCommitOrThrow404(branchId, commitOpt, cache = false)
    val asset     = webUtils.nodeOrThrow404(workspace, name.toString)
    val course    = nodeService.loadA[Course](workspace).byName(workspace.homeName).get
    val isCourse  = asset.info.name == course.info.name

    val mimeType = if doc then GediaType.HTML_UTF_8 else GediaType.ZIP
    val suffix   = if doc then ".html" else ".zip"
    val out      = ExportFile.create(
      s"${course.data.title.trim}${isCourse !? s" - ${asset.title.orZ.trim}"}${suffix}",
      mimeType,
      request
    )

    if doc then htmlExportService.exportHtmlDoc(out.file, asset, course, workspace)
    else htmlExportService.exportHtmlZip(out.file, asset, workspace)

    FileResponse(out.toFileInfo)
  end exportHtml
end HtmlExportWebController
