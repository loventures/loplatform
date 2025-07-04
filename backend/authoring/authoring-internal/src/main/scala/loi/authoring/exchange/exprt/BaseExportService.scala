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

package loi.authoring.exchange.exprt

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.google.common.io.ByteStreams
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.operation.Operations
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.*
import com.learningobjects.cpxp.util.task.Priority
import com.learningobjects.de.task.{TaskReport, TaskReportService, UnboundedTaskReport}
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.edge.EdgeService
import loi.authoring.exchange.exprt.store.{ExportReceiptDao, ExportReceiptEntity}
import loi.authoring.exchange.imprt.File2BlobService
import loi.authoring.exchange.model.{
  ExchangeManifest,
  ExportableExchangeManifest,
  ExportableNodeExchangeData as ExportableNode
}
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.WorkspaceService
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus
import loi.cp.i18n.AuthoringBundle
import org.apache.commons.lang3.StringUtils.trimToNull
import org.hibernate.Session
import scalaz.Validation

import java.io.{BufferedOutputStream, IOException, InputStream}
import java.nio.file.{Files, Path, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}
import java.util.{Date, UUID}

@Service
class BaseExportService(
  blobService: BlobService,
  exportReceiptDao: ExportReceiptDao,
  workspaceService: WorkspaceService,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  file2BlobService: File2BlobService,
  userDto: => UserDTO,
  domainDto: => DomainDTO,
  sessionProvider: () => Session
) extends ExportService:
  import BaseExportService.*

  override def loadExportReceipt(id: Long): Option[ExportReceipt] =
    exportReceiptDao.loadReceipt(id).map(ExportReceiptDao.entityToReceipt)

  override def loadExportReceipts(limit: Int, offset: Int): Seq[ExportReceipt] =
    exportReceiptDao.loadReceipts(limit, offset).map(ExportReceiptDao.entityToReceipt)

  override def doExport(dto: ExportDto): ExportReceipt =
    val entity  = newReceiptEntity(dto)
    exportReceiptDao.save(entity)
    val receipt = ExportReceiptDao.entityToReceipt(entity)

    createExportOp(receipt, dto).perform()
    ExportReceiptDao.entityToReceipt(exportReceiptDao.loadReceipt(receipt.id).get)

  override def deferExport(dto: ExportDto): ExportReceipt =
    val entity  = newReceiptEntity(dto)
    exportReceiptDao.save(entity)
    val receipt = ExportReceiptDao.entityToReceipt(entity)

    val op = createExportOp(receipt, dto)

    Operations.deferTransact(op, Priority.High, s"export-(${receipt.id})")

    receipt
  end deferExport

  private def createExportOp(receipt: ExportReceipt, dto: ExportDto): ExportOperation =
    TaskReportService.track(receipt.report)
    ExportOperation(receipt.copy(), dto)(
      exportReceiptDao,
      workspaceService,
      nodeService,
      edgeService,
      this,
      sessionProvider
    )
  end createExportOp

  private def newReceiptEntity(dto: ExportDto): ExportReceiptEntity =
    val report     = new UnboundedTaskReport(s"Export: ${dto.name}")
    val reportJson = JacksonUtils.getFinatraMapper.valueToTree[JsonNode](report)

    new ExportReceiptEntity(
      null,
      dto.webRequestJson,
      null,
      reportJson,
      null,
      AssetExchangeRequestStatus.Requested.entryName,
      new Date(),
      null,
      null,
      userDto.id,
      domainDto.id
    )
  end newReceiptEntity

  override def deleteExportReceipt(receipt: ExportReceipt): Unit =
    val entity = exportReceiptDao.loadReference(receipt)
    exportReceiptDao.delete(entity)

  private def createZipPath(manifest: ExportableExchangeManifest, report: TaskReport, guid: String): Path =
    val zipPath = Files.createTempFile(s"export-$guid", ".zip") // todo: name of file?
    zipPath.toFile.deleteOnExit()

    var zip: ZipOutputStream = null
    try
      zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))
      val batches       = manifest.nodes.filter(_.hasAttachment).grouped(1000).toSeq
      val filenameState = batches.foldLeft(EmptyFilenameState)((batchesAcc, batch) =>
        val nextAcc = batch.foldLeft(batchesAcc)({ case (batchAcc, node) =>
          writeAttachment(node, zip, batchAcc, report)
        })
        EntityContext.flushClearAndCommit()
        // `writeAttachment` adds to `batchAcc` which adds to `batchesAcc`, so we have
        // nothing to accumulate out here, just pass what we have along
        nextAcc
      )

      val manifest2 = replaceAttachmentFilenames(manifest.jsonManifest, filenameState)
      ZipUtils.writeJsonZipEntry(zip, "manifest.json", manifest2)
      zipPath
    finally if zip != null then zip.close()
    end try
  end createZipPath

  override def buildExportBlobRef(
    manifest: ExportableExchangeManifest,
    report: TaskReport
  ): Validation[Throwable, BlobRef] =
    val guid       = UUID.randomUUID().toString
    val path: Path = createZipPath(manifest, report, guid)
    file2BlobService.putBlob(path.toFile)

  private def writeAttachment(
    node: ExportableNode,
    zip: ZipOutputStream,
    filenameState: FilenameState,
    report: TaskReport,
  ): FilenameState =

    val blobInfo          = getBlobInfo(node, report)
    val nextFilenameState = blobInfo.foldLeft(filenameState)((_, blob) =>

      def putZipEntry(filename: String): Unit =
        var attachment: InputStream = null
        try
          attachment = blob.openInputStream()
          val path = Paths.get("attachments").resolve(filename).toString
          zip.putNextEntry(new ZipEntry(path))
          ByteStreams.copy(attachment, zip)
          zip.closeEntry()
        catch
          case ex: IOException =>
            report.addWarning(
              AuthoringBundle
                .message("export.attachmentIoException", ex.getMessage, blob.getNiceName, node.id)
            )
        finally if attachment != null then attachment.close()
        end try
      end putZipEntry

      val (nextFnState, filename) = uniqueFilename(node.id, blob, filenameState)
      filename match
        case Some(fname) =>
          putZipEntry(fname)
        case None        =>
          report.addWarning(AuthoringBundle.message("export.attachment.missingFilename", node.id))
      nextFnState
    )

    report.markProgress()
    nextFilenameState
  end writeAttachment

  private def getBlobInfo(node: ExportableNode, report: TaskReport): Option[BlobInfo] =
    val source = Option(node.data.get("source"))
    if source.isDefined then source.map(s => blobService.ref2Info(Mapper.convertValue(s, classOf[BlobRef])))
    else None

  private def uniqueFilename(
    nodeImportId: String,
    info: BlobInfo,
    filenameState: FilenameState,
  ): (FilenameState, Option[String]) =
    val fileName             = info.getNiceName
    val nextStateAndFilename = Option(trimToNull(fileName)).map(trimmedName =>

      /** when bypass is on, we want to add a replacement unconditionally so that when [[replaceAttachmentFilenames]] is
        * called the node's "attachment" property points to the file in attachments directory of the zip file
        */
      val newName = StringUtils.appendFilename(fileName, s"-${GuidUtil.shortGuid}")
      (filenameState.addReplacement(nodeImportId, newName), newName)
    )

    nextStateAndFilename match
      case Some((nfs, filename)) => (nfs, Some(filename))
      case None                  => (filenameState, None)
  end uniqueFilename

  private def replaceAttachmentFilenames(
    manifest: ExchangeManifest,
    state: FilenameState
  ): ExchangeManifest =

    val correctedNodes = manifest.nodes.map(node =>
      state.replacements.get(node.id) match
        case Some(newFilename) => node.copy(attachment = Some(newFilename))
        case None              => node
    )

    manifest.copy(nodes = correctedNodes)
  end replaceAttachmentFilenames
end BaseExportService

private object BaseExportService:
  val EmptyFilenameState: FilenameState = FilenameState(Set.empty, Map.empty)
  val Mapper: ObjectMapper              = JacksonUtils.getFinatraMapper

  /** @param replacements
    *   filenames that had to be changed to prevent filename collision in the zip directory. Map of import id to new
    *   filename
    */
  case class FilenameState(
    existingFilenames: Set[String],
    replacements: Map[String, String]
  ):

    def addFilename(filename: String): FilenameState =
      copy(
        existingFilenames = existingFilenames + filename
      )

    def addReplacement(nodeImportId: String, filename: String): FilenameState =
      copy(
        existingFilenames = existingFilenames + filename,
        replacements = replacements.updated(nodeImportId, filename)
      )
  end FilenameState
end BaseExportService
