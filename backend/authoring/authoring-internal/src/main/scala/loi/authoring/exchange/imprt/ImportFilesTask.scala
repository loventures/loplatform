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

import java.io.FileInputStream
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.util.ImageUtils
import com.learningobjects.de.task.TaskReport
import com.learningobjects.de.web.MediaType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.exchange.imprt.store.ImportReceiptDao
import loi.authoring.exchange.model.*

import scala.util.Using

/** For each asset in a zip, if it has a file, put it in the blobstore or in the attachmentfinder table. Pass it back to
  * be saved with the asset in the assetnode table.
  */
class ImportFilesTask private (
  report: TaskReport,
  receipt: ImportReceipt,
  manifest: ValidatedExchangeManifest
)(
  importReceiptDao: ImportReceiptDao,
  blobService: BlobService,
) extends ImportTask[ImportableExchangeManifest](report, receipt)(importReceiptDao):

  override protected def run(): Option[ImportableExchangeManifest] =

    val importableNodes = manifest.nodes.map(node =>

      val importableData = node.file.map({ case ValidatedFileData(filename, mediaType, file) =>
        report.markProgress()
        val upload = new UploadInfo(filename, mediaType.toString, file, false)
        if node.assetType.id == AssetTypeId.Image && (upload.getWidth == null || upload.getHeight == null) && mediaType != MediaType.APPLICATION_SVG
        then
          val dimensions = ImageUtils.getImageDimensions(file)
          upload.setWidth(dimensions.getWidth.toLong)
          upload.setHeight(dimensions.getHeight.toLong)
        Using.resource(new FileInputStream(upload.getFile)) { stream =>
          val blobName = blobService.createBlobName(stream, "authoring/")
          val provider = blobService.getDefaultProvider
          val blobRef  = blobService.putBlob(provider, blobName, upload).get
          ImportedFileData(filename, Some(blobRef))
        }
      })

      node.toImportableData(importableData)
    )

    Some(ImportableExchangeManifest(importableNodes, manifest.setRootAndHome, manifest.competencyIds))
  end run
end ImportFilesTask

object ImportFilesTask:

  def apply(
    receipt: ImportReceipt,
    manifest: ValidatedExchangeManifest
  )(
    importReceiptDao: ImportReceiptDao,
    blobService: BlobService,
  ): ImportFilesTask =
    val report = receipt.report.addChild("Importing Files", manifest.numFiles)
    new ImportFilesTask(report, receipt, manifest)(importReceiptDao, blobService)
end ImportFilesTask
