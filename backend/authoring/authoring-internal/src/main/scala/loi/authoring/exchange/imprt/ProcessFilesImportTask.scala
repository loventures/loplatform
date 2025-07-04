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

import java.io.InputStream
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.util.TempFileMap
import com.learningobjects.cpxp.util.lookup.{FileLookup, FileLookups}
import com.learningobjects.de.task.TaskReport
import loi.authoring.blob.BlobService
import loi.authoring.exchange.imprt.exception.MissingManifestException
import loi.authoring.exchange.imprt.store.ImportReceiptDao
import loi.authoring.exchange.model.ExchangeManifest

import scala.util.control.NonFatal
import scala.compat.java8.OptionConverters.*
import scala.util.Using
import scala.util.Using.Releasable

class ProcessFilesImportTask private (
  report: TaskReport,
  receipt: ImportReceipt,
  dto: ConvertedImportDto
)(
  importReceiptDao: ImportReceiptDao,
  blobService: BlobService
) extends ImportTask[(FileLookup, ExchangeManifest)](
      report,
      receipt
    )(importReceiptDao):

  override protected def run(): Option[(FileLookup, ExchangeManifest)] =
    transformResource(unzipImportPackage()) { files =>
      Some((files, readManifest(files)))
    }

  /** Transform a resource, closing the resource if the transform fails. In a more principled world this would be a
    * flatMap.
    */
  private def transformResource[A: Releasable, B](a: => A)(f: A => B): B =
    try f(a)
    catch
      case NonFatal(e) =>
        implicitly[Releasable[A]].release(a)
        throw e

  private def unzipImportPackage(): FileLookup =
    Using.resource(openDto()) { in =>
      val tfm = new TempFileMap(s"import-${receipt.id}-", ".tmp")
      tfm.importZip(in)
      FileLookups.lookup(tfm)
    }

  private def openDto(): InputStream = blobService.ref2Stream(dto.convertedSource)

  private def readManifest(files: FileLookup): ExchangeManifest =
    val manifestFile = files
      .get("manifest.json")
      .asScala
      .getOrElse(throw MissingManifestException)
    var manifest     = JacksonUtils.getFinatraMapper.readValue(manifestFile, classOf[ExchangeManifest])

    if manifest.nodes == null then manifest = manifest.copy(nodes = Seq.empty)

    if manifest.competencyIds == null then manifest = manifest.copy(competencyIds = Set.empty)

    manifest
  end readManifest
end ProcessFilesImportTask

object ProcessFilesImportTask:

  def apply(receipt: ImportReceipt, dto: ConvertedImportDto)(
    importReceiptDao: ImportReceiptDao,
    blobService: BlobService
  ): ProcessFilesImportTask =
    val report = receipt.report.addChild("Processing Files")
    new ProcessFilesImportTask(report, receipt, dto)(importReceiptDao, blobService)
