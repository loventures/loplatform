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

import com.learningobjects.de.task.TaskReport
import loi.authoring.blob.BlobRef
import loi.authoring.exchange.exprt.store.ExportReceiptDao
import loi.authoring.exchange.model.ExportableExchangeManifest
import scalaz.Validation

class ExportBlobRefTask(report: TaskReport, receipt: ExportReceipt, manifest: ExportableExchangeManifest)(
  exportService: ExportService,
  exportReceiptDao: ExportReceiptDao
) extends ExportTask[BlobRef](report, receipt)(exportReceiptDao):
  override protected def run(): Validation[Throwable, BlobRef] =
    exportService.buildExportBlobRef(manifest, report)

object ExportBlobRefTask:
  def apply(receipt: ExportReceipt, manifest: ExportableExchangeManifest)(
    exportReceiptDao: ExportReceiptDao,
    exportService: ExportService
  ): ExportBlobRefTask =

    val numBlobRefs = manifest.nodes.count(_.hasAttachment)
    val report      = receipt.report.addChild("Exporting BlobRefs", numBlobRefs)
    new ExportBlobRefTask(report, receipt, manifest)(exportService, exportReceiptDao)
