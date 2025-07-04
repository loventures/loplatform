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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.authorization.Secured
import loi.asset.util.Assex.*
import loi.authoring.edge.EdgeService
import loi.authoring.project.AccessRestriction
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.WorkspaceService

import java.util.UUID

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[html] class HtmlImportWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
  workspaceService: WorkspaceService,
  htmlImportService: HtmlImportService,
)(implicit
  edgeService: EdgeService,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "authoring/{branch}/asset/{name}/import/html", method = Method.GET)
  def validateHtmlZip(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: UUID,
    @QueryParam upload: UploadInfo
  ): ArgoResponse[List[String]] =
    val workspace = webUtils.workspaceOrThrow404(branchId, cache = false)
    val asset     = webUtils.nodeOrThrow404(workspace, name)

    ArgoResponse(htmlImportService.validateHtmlZip(upload.getFile, asset, workspace).flatMap(_.title))
  end validateHtmlZip

  @RequestMapping(path = "authoring/{branch}/asset/{name}/import/html", method = Method.POST)
  def importHtmlZip(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: UUID,
    @QueryParam upload: UploadInfo
  ): ArgoResponse[Int] =
    val branch    = webUtils.branchOrFakeBranchOrThrow404(branchId)
    val workspace = workspaceService.requireWriteWorkspace(branch.id, AccessRestriction.projectMember)
    val asset     = webUtils.nodeOrThrow404(workspace, name)

    ArgoResponse(htmlImportService.importHtmlZip(upload.getFile, asset, workspace).modifiedNodes.size)
  end importHtmlZip

  @RequestMapping(path = "authoring/{branch}/asset/{name}/import/document", method = Method.GET)
  def validateHtmlDoc(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: UUID,
    @QueryParam upload: UploadInfo
  ): PageImportValidation =
    val workspace = webUtils.workspaceOrThrow404(branchId, cache = false)
    val asset     = webUtils.nodeOrThrow404(workspace, name)

    htmlImportService.validateHtmlDoc(upload.getFile, asset, workspace)
  end validateHtmlDoc

  @RequestMapping(path = "authoring/{branch}/asset/{name}/import/document", method = Method.POST)
  def importHtmlDoc(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: UUID,
    @QueryParam upload: UploadInfo
  ): Unit =
    val branch    = webUtils.branchOrFakeBranchOrThrow404(branchId)
    val workspace = workspaceService.requireWriteWorkspace(branchId, AccessRestriction.projectMember)
    val asset     = webUtils.nodeOrThrow404(workspace, name)

    htmlImportService.importHtmlDoc(upload.getFile, asset, workspace)
  end importHtmlDoc
end HtmlImportWebController
