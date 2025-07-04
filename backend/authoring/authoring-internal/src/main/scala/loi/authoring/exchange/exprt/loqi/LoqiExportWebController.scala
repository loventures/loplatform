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

package loi.authoring.exchange.exprt.loqi

import com.google.common.net.MediaType as GediaType
import com.learningobjects.cpxp.component.annotation.{Component, Controller, PathVariable, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, FileResponse, Method, WebRequest}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import loi.asset.util.Assex.*
import loi.authoring.edge.EdgeService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import scalaz.syntax.std.option.*

import java.io.FileOutputStream
import scala.util.Using

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[loqi] class LoqiExportWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
  loqiExportService: LoqiExportService,
)(implicit edgeService: EdgeService)
    extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "authoring/{branch}/asset/{asset}/export/loqi", method = Method.GET)
  def exportHtml(
    @PathVariable("branch") branchId: Long,
    @PathVariable("asset") assetName: String,
    request: WebRequest
  ): FileResponse[?] = exportHtmlImpl(branchId, None, assetName, request)

  @RequestMapping(path = "authoring/{branch}/commits/{commit}/asset/{asset}/export/loqi", method = Method.GET)
  def exportHtml(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commit") commitId: Long,
    @PathVariable("asset") assetName: String,
    request: WebRequest
  ): FileResponse[?] = exportHtmlImpl(branchId, Some(commitId), assetName, request)

  private def exportHtmlImpl(
    branchId: Long,
    commitOpt: Option[Long],
    assetName: String,
    request: WebRequest
  ): FileResponse[?] =
    val workspace = webUtils.workspaceOptionallyAtCommitOrThrow404(branchId, commitOpt, cache = false)
    val quiz      = webUtils.nodeOrThrow404(workspace, assetName)

    val out = ExportFile.create(s"${quiz.title.cata(_.trim, "Assessment")}.docx", GediaType.OOXML_DOCUMENT, request)
    Using.resource(new FileOutputStream(out.file)) { out =>
      loqiExportService.exportQuiz(workspace, quiz, out)
    }

    FileResponse(out.toFileInfo)
  end exportHtmlImpl
end LoqiExportWebController
