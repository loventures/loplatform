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

package loi.authoring.exchange.imprt

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.operation.Operations
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.task.Priority
import com.learningobjects.de.task.{TaskReportService, UnboundedTaskReport}
import loi.authoring.blob.BlobService
import loi.authoring.exchange.imprt.store.ImportReceiptDao
import loi.authoring.node.AssetNodeService
import loi.authoring.validate.ValidationService
import loi.authoring.workspace.WorkspaceService
import loi.authoring.write.WriteService
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus

@Service
class BaseImportService(
  mimeWebService: MimeWebService,
  nodeService: AssetNodeService,
  workspaceService: WorkspaceService,
  writeService: WriteService,
  importReceiptDao: ImportReceiptDao,
  domainDto: => DomainDTO,
  userDto: => UserDTO,
  validationService: ValidationService,
  blobService: BlobService,
) extends ImportService:

  override def loadImportReceipt(id: Long): Option[ImportReceipt] =
    importReceiptDao.loadReceipt(id).map(ImportReceiptDao.entityToReceipt)

  override def loadImportReceipts(limit: Int, offset: Int): Seq[ImportReceipt] =
    importReceiptDao.loadReceipts(limit, offset).map(ImportReceiptDao.entityToReceipt)

  override def deferImport(dto: ConvertedImportDto, existingReceipt: Option[ImportReceipt]): ImportReceipt =
    importInternal(dto, defer = true, existingReceipt)

  override def doImport(dto: ConvertedImportDto): ImportReceipt =
    importInternal(dto, defer = false, existingReceipt = None)

  override def recordValidation(
    dto: ValidatedImportDto
  ): ImportReceipt =
    val entity = ImportReceiptDao.newEntity(
      dto.importName,
      dto.dataJson,
      dto.downloadFilename,
      dto.convertedSource,
      Some(dto.unconvertedSource),
      AssetExchangeRequestStatus.Validated,
      userDto.id,
      domainDto.id,
      dto.taskReport,
      Some(dto.taskReport.getStartTime)
    )
    importReceiptDao.save(entity)
    ImportReceiptDao.entityToReceipt(entity)
  end recordValidation

  private def importInternal(
    dto: ConvertedImportDto,
    defer: Boolean,
    existingReceipt: Option[
      ImportReceipt
    ] // If we are mutating an existing importreceipt created during validation step
  ): ImportReceipt =

    val entity  = existingReceipt match
      case Some(rcpt) =>
        val entity = ImportReceiptDao.receiptToEntity(
          rcpt.updated(dto.convertedSource, dto.dataJson, AssetExchangeRequestStatus.Requested)
        )
        importReceiptDao.merge(entity)
        entity
      case None       =>
        val entity = ImportReceiptDao.newEntity(
          dto.importName,
          dto.dataJson,
          dto.convertedSource.filename,
          Some(dto.convertedSource),
          unconvertedSource = None, // an already-baked LOAF won't have an unconverted source
          AssetExchangeRequestStatus.Requested,
          userDto.id,
          domainDto.id,
          new UnboundedTaskReport(s"Import: ${dto.importName}"),
          startTime = None
        )
        importReceiptDao.save(entity)
        entity
    val receipt = ImportReceiptDao.entityToReceipt(entity)
    TaskReportService.track(receipt.report)

    val op = new ImportOperation(receipt.copy(), dto)(
      mimeWebService,
      nodeService,
      workspaceService,
      writeService,
      importReceiptDao,
      validationService,
      blobService,
    )

    if defer then
      Operations.deferTransact(op, Priority.High, s"import-(${receipt.id})")
      receipt
    else
      op.perform()
      loadImportReceipt(receipt.id).get
  end importInternal

  override def deleteImportReceipt(receipt: ImportReceipt): Unit =
    val entity = importReceiptDao.loadReference(receipt)
    importReceiptDao.delete(entity)
end BaseImportService
