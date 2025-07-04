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

package loi.authoring.exchange.imprt.imscc

import java.io.{File, FileInputStream, InputStream}
import java.nio.file.Path

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.lookup.FileLookups
import com.learningobjects.cpxp.util.{GuidUtil, TempFileMap}
import com.learningobjects.de.task.TaskReport
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.*
import loi.authoring.exchange.imprt.web.ExchangeReportDto
import loi.authoring.exchange.imprt.{ImporterUtils, NodeExchangeBuilder, ThirdPartyImportType}
import loi.authoring.exchange.model.ExchangeManifest
import loi.cp.i18n.{AuthoringBundle, BundleMessage}
import scala.util.Using
import scalaz.ValidationNel
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.OptionOps.*

import scala.xml.Elem

/** Imports Common Cartridge files as LO courses.
  */
@Service
class CommonCartridgeImporter(
  mapper: ObjectMapper,
  moduleImporter: CommonCartridgeModuleImporter,
  resourceImporter: CommonCartridgeResourceImporter
):
  import CommonCartridgeImporter.*

  def importZip(inputZip: File, outputPath: Path): Unit =
    Using.resources(
      new TempFileMap(s"imscc_${inputZip.getName}_${GuidUtil.shortGuid()}", ".tmp"),
      new FileInputStream(inputZip)
    ) { (files, is) =>
      // in theory this could fail, but the only caller should have validated already beforehand
      val result       = validateZip(files, is, persistNewLtiTools = true)
      val manifestFile = writeManifestFile(result.manifest.get, mapper)
      writeExchangeZip(outputPath, manifestFile, FileLookups.lookup(files))
    }

  def validateZip(
    files: TempFileMap,
    unconvertedStream: InputStream,
    persistNewLtiTools: Boolean = false,
    taskReport: TaskReport = ImporterUtils.createValidatingReport
  ): ExchangeReportDto =
    import scalaz.Validation.FlatMap.*
    val attempt = for
      _            <- ImporterUtils.readZip(ThirdPartyImportType.CommonCartridge, files, unconvertedStream)
      manifestFile <- validateManifestExists(files)
      manifestXml  <- ImporterUtils.loadXml(manifestFile, ThirdPartyImportType.CommonCartridge)
      _            <- validateManifest(manifestXml)
    yield
      val courseTitle     = (manifestXml \ "metadata" \ "lom" \ "general" \ "title" \ "string").text.trim
      val items           = manifestXml \ "organizations" \ "organization" \\ "item"
      val itemResources   = items.filter(item => (item \ "@identifierref").text.nonEmpty)
      val itemResourceIds = itemResources.map(item => (item \ "@identifierref").text).toSet
      val itemTitles      = itemResources.map(item => (item \ "@identifierref").text -> (item \ "title").text).toMap

      val resourceNodes =
        (manifestXml \ "resources" \ "resource").filter(r => itemResourceIds.contains((r \ "@identifier").text))
      val resources     = resourceImporter.buildResources(resourceNodes, itemTitles, files, taskReport, persistNewLtiTools)

      val modules = moduleImporter.buildModules(manifestXml, resources)

      val course = NodeExchangeBuilder
        .builder(guid, AssetTypeId.Course.entryName)
        .title(courseTitle)
        .edges(buildEmptyEdgesFromAssets(modules.map(_.node).toList, Group.Elements))
        .build()

      val assets = modules.flatMap(_.family).toList ::: List(course)

      val manifest = ExchangeManifest.empty.copy(nodes = assets)
      ExchangeReportDto(Some(manifest), taskReport)
    taskReport.markComplete()
    attempt.valueOr(bmNel =>
      bmNel.foreach(bm => taskReport.addError(bm))
      ExchangeReportDto(None, taskReport)
    )
  end validateZip

  private def validateManifestExists(files: TempFileMap): ValidationNel[BundleMessage, File] =
    Option(files.get(IMSManifestXML)).elseInvalidNel(AuthoringBundle.message("imscc.import.manifestNotFound"))

  private def validateManifest(manifestXml: Elem): ValidationNel[BundleMessage, Unit] =
    val schema  = (manifestXml \ "metadata" \ "schema").text
    val version = (manifestXml \ "metadata" \ "schemaversion").text
    isSupportedSchemaVersion(schema, version)
      .elseInvalidNel(AuthoringBundle.message("imscc.import.unsupportedSchema", schema, version))

  private def isSupportedSchemaVersion(schema: String, version: String): Boolean =
    supportedVersions.get(schema).exists(_.contains(version))
end CommonCartridgeImporter

object CommonCartridgeImporter:
  val IMSManifestXML    = "imsmanifest.xml"
  val supportedVersions = Map(
    "IMS Common Cartridge"      -> Set("1.1.0", "1.2.0"),
    "IMS Thin Common Cartridge" -> Set("1.3.0")
  )
