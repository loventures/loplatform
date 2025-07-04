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

package loi.authoring.exchange.exprt.web

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.web.{ApiRootComponent, FileResponse, Method, WebResponse}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.attachment.Disposition
import com.learningobjects.cpxp.service.exception.HttpApiException
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.{BlobInfo, LocalFileInfo}
import com.learningobjects.de.authorization.Secured
import loi.authoring.asset.Asset
import loi.authoring.blob.BlobService
import loi.authoring.exchange.exprt.loqi.LoqiExportService
import loi.authoring.exchange.exprt.{CourseStructureExportService, ExportDto, ExportReceipt, ExportService}
import loi.authoring.node.AssetNodeService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.ReadWorkspace
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus
import loi.cp.i18n.AuthoringBundle
import loi.cp.user.UserService
import scaloi.syntax.`try`.*

import java.util.UUID
import scala.util.Try

@Component
@Controller(value = "authoringExport", root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[web] class ExportWebController(
  ci: ComponentInstance,
  exportService: ExportService,
  nodeService: AssetNodeService,
  blobService: BlobService,
  user: => UserDTO,
  userService: UserService,
  webUtils: AuthoringWebUtils,
  loqiExportService: LoqiExportService,
  courseStructureExportService: CourseStructureExportService
) extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(path = "authoring/exports", method = Method.POST)
  def createExportReceipt(@RequestBody webDto: ExportWebDto): ExportReceiptsResponseDto =
    val dto     = exportDto(webDto)
    val receipt = exportService.deferExport(dto)
    ExportReceiptsResponseDto(Seq(receipt), Map(user.id -> user))
  end createExportReceipt

  @RequestMapping(path = "authoring/exports/async", method = Method.POST, async = true)
  def exportAsync(@RequestBody webDto: ExportWebDto): ExportReceiptsResponseDto =
    val dto     = exportDto(webDto)
    val receipt = exportService.doExport(dto)
    ExportReceiptsResponseDto(Seq(receipt), Map(user.id -> user))
  end exportAsync

  private def exportDto(webDto: ExportWebDto) =
    val workspace  = webUtils.workspaceOrThrow404(webDto.branchId)
    val nodes      = requireNodes(workspace, webDto.rootNodeNames)
    val webDtoJson = JacksonUtils.getFinatraMapper.valueToTree[JsonNode](webDto)
    ExportDto(webDto.name, webDtoJson, workspace.branch, nodes)

  @RequestMapping(path = "authoring/exports", method = Method.GET)
  def getExportReceipts(@QueryParam limit: Int, @QueryParam offset: Int): ExportReceiptsResponseDto =
    val receipts = exportService.loadExportReceipts(limit, offset)
    val users    = userService.getUsers(receipts.flatMap(_.createdBy))
    ExportReceiptsResponseDto(receipts, users)

  @RequestMapping(path = "authoring/exports/{id}", method = Method.GET)
  def getExportReceipt(@PathVariable("id") id: Long): ExportReceiptsResponseDto =
    val receipt = loadReceiptOrNotFound(id)
    val users   = userService.getUsers(receipt.createdBy)
    ExportReceiptsResponseDto(Seq(receipt), users)

  @RequestMapping(path = "authoring/exports/{id}/package", method = Method.GET)
  def serveExportPackage(@PathVariable("id") id: Long): WebResponse =

    val receipt = loadReceiptOrNotFound(id)

    lazy val notAvailMsgKey = receipt.status match
      case AssetExchangeRequestStatus.Failure  => "export.packageNotAvailable.failure"
      case AssetExchangeRequestStatus.Underway => "export.packageNotAvailable.tooEarly"
      case _                                   => "export.packageNotAvailable.queued"

    val receiptBlobInfo: BlobInfo = receipt.source
      .map(src => blobService.ref2Info(src))
      .getOrElse(
        throw HttpApiException
          .unprocessableEntity(AuthoringBundle.message(notAvailMsgKey))
      )

    FileResponse.apply(receiptBlobInfo)
  end serveExportPackage

  @RequestMapping(path = "authoring/exports/{id}", method = Method.DELETE)
  def deleteExportReceipt(@PathVariable("id") id: Long): Unit =
    val receipt = loadReceiptOrNotFound(id)
    exportService.deleteExportReceipt(receipt)

  @RequestMapping(path = "authoring/{branchId}/assessmentExport", method = Method.GET)
  def exportProject(@PathVariable("branchId") branchId: Long): WebResponse =
    exportProjectImpl(branchId, None)

  @RequestMapping(path = "authoring/{branchId}/commits/{commitId}/assessmentExport", method = Method.GET)
  def exportProject(@PathVariable("branchId") branchId: Long, @PathVariable("commitId") commitId: Long): WebResponse =
    exportProjectImpl(branchId, Some(commitId))

  private def exportProjectImpl(branchId: Long, commitOpt: Option[Long]): WebResponse =
    val workspace = webUtils.workspaceOptionallyAtCommitOrThrow404(branchId, commitOpt)
    val zipInfo   = loqiExportService.doExport(workspace).mapExceptions(AuthoringWebUtils.AsApiException).get

    val info = new LocalFileInfo(zipInfo.getFile)
    info.setContentType(zipInfo.getMimeType)
    info.setDisposition(Disposition.attachment, zipInfo.getFileName)
    FileResponse(info)

  @RequestMapping(path = "authoring/{branch}/csvExport", method = Method.GET)
  def exportCsv(@PathVariable("branch") branchId: Long): WebResponse =
    exportCsvImpl(branchId, None)

  @RequestMapping(path = "authoring/{branch}/commits/{commit}/csvExport", method = Method.GET)
  def exportCsv(@PathVariable("branch") branchId: Long, @PathVariable("commit") commitId: Long): WebResponse =
    exportCsvImpl(branchId, Some(commitId))

  private def exportCsvImpl(branchId: Long, commitOpt: Option[Long]): WebResponse =
    val workspace = webUtils.workspaceOptionallyAtCommitOrThrow404(branchId, commitOpt, cache = false)
    val csvExport =
      Try(courseStructureExportService.exportStructure(workspace)).mapExceptions(AuthoringWebUtils.AsApiException).get

    val info = new LocalFileInfo(csvExport.getFile)
    info.setContentType(csvExport.getMimeType)
    info.setDisposition(Disposition.attachment, csvExport.getFileName)
    FileResponse(info)

  private def loadReceiptOrNotFound(id: Long): ExportReceipt =
    exportService
      .loadExportReceipt(id)
      .getOrElse(
        throw HttpApiException
          .notFound(AuthoringBundle.message("export.missingRequest", long2Long(id)))
      )

  private def requireNodes(workspace: ReadWorkspace, names: Seq[UUID]): Seq[Asset[?]] =
    nodeService
      .load(workspace)
      .byName(names)
      .recover({ case ex: NoSuchNodeInWorkspaceException =>
        throw HttpApiException
          .unprocessableEntity(ex)
      })
      .get
end ExportWebController
