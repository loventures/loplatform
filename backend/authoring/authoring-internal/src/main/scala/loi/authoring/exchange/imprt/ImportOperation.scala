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

import com.learningobjects.cpxp.operation.UncheckedVoidOperation
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.util.ManagedUtils
import com.learningobjects.cpxp.util.lookup.FileLookup
import com.learningobjects.de.task.TaskReportService
import loi.authoring.blob.BlobService
import loi.authoring.exchange.imprt.reconcile.ReconcileExpectedAssetsImportTask
import loi.authoring.exchange.imprt.store.ImportReceiptDao
import loi.authoring.exchange.imprt.validation.ValidateImportTask
import loi.authoring.exchange.model.{
  ExchangeManifest,
  ImportableExchangeManifest,
  ImportedRoot,
  ValidatedExchangeManifest
}
import loi.authoring.node.AssetNodeService
import loi.authoring.project.AccessRestriction
import loi.authoring.validate.ValidationService
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace, WorkspaceService}
import loi.authoring.write.WriteService
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus
import org.hibernate.Session

import java.util.{Date, UUID}
import scala.util.Using

class ImportOperation(
  receipt: ImportReceipt,
  dto: ConvertedImportDto
)(
  mimeWebService: MimeWebService,
  nodeService: AssetNodeService,
  workspaceService: WorkspaceService,
  writeService: WriteService,
  importReceiptDao: ImportReceiptDao,
  validationService: ValidationService,
  blobService: BlobService,
) extends UncheckedVoidOperation:

  private var session: Session = scala.compiletime.uninitialized

  override protected def execute(): Unit =

    // ImportOperation is usually deferred to a background thread. It cannot use the
    // session of the spawning thread
    session = ManagedUtils.getEntityContext.getEntityManager.unwrap(classOf[Session])

    markStart()
    TaskReportService.track(receipt.report)

    for (files0, manifest) <- readManifest
    yield Using.resource(files0) { files =>
      val targetWs = workspaceService.requireReadWorkspace(dto.target.id, AccessRestriction.none)
      for
        validManifest       <- validate(files, manifest, targetWs)
        expectedAssets      <- findExpectedAssets(validManifest, targetWs)
        attImportedManifest <- writeAttachments(validManifest)
        importedRoots       <- importNodesAndEdges(attImportedManifest, expectedAssets, targetWs)
      do
        receipt.importedRoots = importedRoots
        markSuccess()
    }
  end execute

  private def readManifest: Option[(FileLookup, ExchangeManifest)] =
    ProcessFilesImportTask(receipt, dto)(importReceiptDao, blobService).runSafe()

  private def validate(
    files: FileLookup,
    manifest: ExchangeManifest,
    targetWs: AttachedReadWorkspace
  ): Option[ValidatedExchangeManifest] =
    ValidateImportTask(receipt, files, manifest, targetWs)(importReceiptDao, mimeWebService, validationService)
      .runSafe()

  private def findExpectedAssets(
    validManifest: ValidatedExchangeManifest,
    readWorkspace: ReadWorkspace
  ): Option[Map[String, UUID]] =
    ReconcileExpectedAssetsImportTask(receipt, validManifest, readWorkspace)(importReceiptDao, session)
      .runSafe()

  private def writeAttachments(
    validManifest: ValidatedExchangeManifest
  ): Option[ImportableExchangeManifest] =
    ImportFilesTask(receipt, validManifest)(importReceiptDao, blobService)
      .runSafe()

  private def importNodesAndEdges(
    manifest: ImportableExchangeManifest,
    expectedAssets: Map[String, UUID],
    targetWs: AttachedReadWorkspace
  ): Option[Seq[ImportedRoot]] =
    ImportContentTask(receipt, manifest, expectedAssets, targetWs)(
      importReceiptDao,
      nodeService,
      workspaceService,
      writeService
    ).runSafe()

  private def markStart(): Unit =
    // There may have been a start time set on the *receipt* before, during validation step
    // but if there was no validation step (we started with a LOAF), then set the start time now
    receipt.startTime.getOrElse({
      val now = new Date()
      receipt.startTime = Some(now)
      receipt.report.markStart(now)
    })
    receipt.status = AssetExchangeRequestStatus.Underway

  private def markSuccess(): Unit =
    val now = new Date()
    receipt.report.markComplete(now) // the parent report
    receipt.endTime = Some(now)
    receipt.status = AssetExchangeRequestStatus.Success
    importReceiptDao.merge(ImportReceiptDao.receiptToEntity(receipt))
end ImportOperation
