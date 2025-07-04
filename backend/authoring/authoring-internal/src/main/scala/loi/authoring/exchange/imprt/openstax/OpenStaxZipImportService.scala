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

package loi.authoring.exchange.imprt.openstax

import java.io.{File, FileInputStream, InputStream}
import java.nio.file.Path

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.lookup.FileLookups
import com.learningobjects.cpxp.util.{GuidUtil, TempFileMap}
import com.learningobjects.de.task.TaskReport
import loi.authoring.exchange.imprt.ImporterUtils
import loi.authoring.exchange.imprt.ImporterUtils.*
import loi.authoring.exchange.imprt.web.ExchangeReportDto
import loi.authoring.exchange.model.ExchangeManifest
import loi.cp.i18n.{AuthoringBundle, BundleMessage}
import scala.util.Using
import scalaz.ValidationNel
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.OptionOps.*

import scala.jdk.CollectionConverters.*
import scala.xml.{Elem, XML}
@Service
class OpenStaxZipImportService(
  mapper: ObjectMapper,
  courseImporter: OpenStaxCourseImportService,
  lessonImporter: OpenStaxLessonImportService
):

  def importZip(
    zipFile: File,
    outputPath: Path
  ): Unit =
    Using.resources(
      new TempFileMap(s"openstax_${zipFile.getName}_${GuidUtil.shortGuid()}", ".tmp"),
      new FileInputStream(zipFile)
    ) { (temp, stream) =>
      val lookup       = FileLookups.lookup(temp)
      // in theory this could fail, but the only caller should have validated already beforehand
      val result       = validateZip(temp, stream)
      val manifestFile = writeManifestFile(result.manifest.get, mapper)
      writeExchangeZip(outputPath, manifestFile, lookup)
    }

  def validateZip(
    files: TempFileMap,
    zipStream: InputStream,
    taskReport: TaskReport = ImporterUtils.createValidatingReport
  ): ExchangeReportDto =
    files.importZip(zipStream)
    val firstFile = files.keySet().asScala.filterNot(_.startsWith("__MACOSX/")).head
    val directory = firstFile.substring(0, firstFile.indexOf("/"))
    import scalaz.Validation.FlatMap.*
    val attempt   = for
      collection <- validateCollectionExists(files, directory)
      _          <- validateModulesExist(files, directory, collection)
    yield
      val licenseAndAuthor = courseImporter.getAuthorAndLicense(collection)

      val lessonFiles    = files.keySet().asScala.filter(_.endsWith("/index.cnxml"))
      val lessonAssetMap = lessonFiles.map { lessonFile =>
        val split      = lessonFile.split("/") /* col12345/m12345/index.cnxml */
        val lessonPath = split.slice(0, 2).mkString("/")
        val lessonId   = split(1)
        val inputFile  = files.get(lessonFile)
        val document   = XML.loadFile(inputFile)
        val assets     = lessonImporter.buildAssetsFromDocument(document, licenseAndAuthor, lessonPath)
        lessonId -> assets
      }.toMap

      val collectionAssets = courseImporter.buildAssetsFromCollection(collection, licenseAndAuthor, lessonAssetMap)
      val allAssets        = lessonAssetMap.values.toList.flatten ++ collectionAssets

      val manifest = ExchangeManifest.empty.copy(nodes = allAssets)
      ExchangeReportDto(Some(manifest), taskReport)
    taskReport.markComplete()
    attempt
      .valueOr(bmNel =>
        bmNel.map(bm => taskReport.addError(bm))
        ExchangeReportDto(None, taskReport)
      )
  end validateZip

  private def validateCollectionExists(
    files: TempFileMap,
    directory: String
  ): ValidationNel[BundleMessage, Elem] =
    Option(files.get(s"$directory/collection.xml"))
      .map(XML.loadFile)
      .elseInvalidNel(AuthoringBundle.message("openstax.import.collectionNotFound"))

  private def validateModulesExist(
    files: TempFileMap,
    directory: String,
    collection: Elem
  ): ValidationNel[BundleMessage, Unit] =
    (collection \ "content" \\ "module")
      .map(m => (m \ "@document").text)
      .map(moduleId =>
        val indexCnxml = s"$directory/$moduleId/index.cnxml"
        files
          .containsKey(indexCnxml)
          .elseInvalidNel(AuthoringBundle.message("openstax.import.moduleNotFound", indexCnxml))
      )
      .head
end OpenStaxZipImportService
