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

package loi.authoring.copy.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.de.authorization.Secured
import loi.authoring.copy.{ContentCopyService, CopyReceipt, CopyService}
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import scaloi.syntax.`try`.*

import java.util.UUID

@Component
@Controller(root = true, value = "copy-web-controller")
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[web] class CopyWebController(
  ci: ComponentInstance,
  webUtils: AuthoringWebUtils,
  copyService: CopyService,
  contentCopyService: ContentCopyService
) extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(path = "authoring/{branch}/nodes/{name}/copy", method = Method.POST)
  def copy(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") nodeName: String,
    @RequestBody webDto: CopyWebDto
  ): CopyReceipt =

    val workspace = webUtils.workspaceOrThrow404(branchId)
    val source    = webUtils.nodeOrThrow404(workspace, nodeName)

    copyService.deferDeepCopy(workspace, source, webDto.newTitle)
  end copy

  @RequestMapping(path = "authoring/nodes/copy", method = Method.POST)
  def copy(@RequestBody copyDto: ContentCopyDto): UUID =
    contentCopyService
      .copy(copyDto.source.plural, copyDto.group, copyDto.target, copyDto.beforeEdge)
      .mapExceptions(AuthoringWebUtils.AsApiException)
      .get
      .head

  @RequestMapping(path = "authoring/nodes/copyBulk", method = Method.POST)
  def copy(@RequestBody copyDto: BulkContentCopyDto): List[UUID] =
    contentCopyService
      .copy(copyDto.source, copyDto.group, copyDto.target, copyDto.beforeEdge)
      .mapExceptions(AuthoringWebUtils.AsApiException)
      .get

  @RequestMapping(path = "authoring/copyReceipts/{id}", method = Method.GET)
  def getReceipt(@PathVariable("id") id: Long): Option[CopyReceipt] =
    copyService.loadReceipt(id)

  @RequestMapping(path = "authoring/copyReceipts/{id}", method = Method.DELETE)
  def deleteReceipt(@PathVariable("id") id: Long): Unit =
    copyService.loadReceipt(id).foreach(copyService.deleteReceipt)
end CopyWebController
