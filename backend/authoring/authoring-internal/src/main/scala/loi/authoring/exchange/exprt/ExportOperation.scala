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

import com.learningobjects.cpxp.operation.UncheckedVoidOperation
import com.learningobjects.de.task.TaskReportService
import loi.authoring.blob.BlobRef
import loi.authoring.edge.EdgeService
import loi.authoring.exchange.exprt.store.ExportReceiptDao
import loi.authoring.exchange.model.ExportableExchangeManifest
import loi.authoring.node.AssetNodeService
import loi.authoring.project.AccessRestriction
import loi.authoring.workspace.{ReadWorkspace, WorkspaceService}
import org.hibernate.Session
import scalaz.Validation
import scalaz.Validation.FlatMap.*

import java.util.UUID

class ExportOperation(
  receipt: ExportReceipt,
  dto: ExportDto
)(
  exportReceiptDao: ExportReceiptDao,
  workspaceService: WorkspaceService,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  exportService: ExportService,
  session: () => Session
) extends UncheckedVoidOperation:

  override protected def execute(): Unit =

    receipt.markStart()
    exportReceiptDao.update(ExportReceiptDao.receiptToEntity(receipt))
    TaskReportService.track(receipt.report)

    val workspace         = workspaceService.requireReadWorkspace(dto.branch.id, AccessRestriction.none)
    val startingNodeNames = dto.nodes.map(_.info.name).toSet

    for
      manifest   <- exportContent(workspace, startingNodeNames)
      exportBlob <- exportBlobRef(manifest)
    yield
      receipt.markSuccess(exportBlob)
      exportReceiptDao.update(ExportReceiptDao.receiptToEntity(receipt))
  end execute

  private def exportContent(
    workspace: ReadWorkspace,
    startingNodeNames: Set[UUID]
  ) =
    ExportContentTask(receipt, workspace, startingNodeNames)(
      exportReceiptDao,
      nodeService,
      edgeService,
      session()
    ).runSafe()

  private def exportBlobRef(manifest: ExportableExchangeManifest): Validation[Throwable, BlobRef] =
    ExportBlobRefTask(receipt, manifest)(exportReceiptDao, exportService).runSafe()
end ExportOperation
