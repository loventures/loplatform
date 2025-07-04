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

package loi.authoring.exchange.imprt.qti

import java.io.{File, FileInputStream}
import java.nio.file.Path
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.util.lookup.FileLookups
import com.learningobjects.cpxp.util.{GuidUtil, TempFileMap}
import com.learningobjects.de.task.TaskReport
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group.Questions
import loi.authoring.exchange.imprt.exception.FatalQtiImportException
import loi.authoring.exchange.imprt.web.{ExchangeReportDto, QtiImportType}
import loi.authoring.exchange.imprt.{ImporterUtils, ThirdPartyImportType}
import loi.authoring.exchange.model.ExchangeManifest
import loi.cp.i18n.AuthoringBundle
import org.log4s.Logger
import scala.util.Using

import java.util.UUID
import scala.xml.Elem

/** Imports QTI ZIP files.
  */
@Service
class QtiImporter(
  mapper: ObjectMapper,
  qti1Importer: Qti1ZipImporter,
  qti2Importer: Qti2ZipImporter,
  assessmentBuilder: AssessmentBuilder
):
  import QtiImporter.*

  def validateAndWriteExchangeZip(
    inputZip: File,
    outputPath: Path,
    qtiImportType: QtiImportType,
    taskReport: TaskReport = ImporterUtils.createValidatingReport,
    competenciesByName: Map[String, UUID] = Map.empty,
  ): ExchangeReportDto =
    Using.resources(newTempFileMap(inputZip.getName), new FileInputStream(inputZip)) { (files, stream) =>
      import scalaz.Validation.FlatMap.*
      val attempt = for
        _            <- ImporterUtils.readZip(ThirdPartyImportType.Qti, files, stream)
        manifestFile <- QtiImportUtils.getManifestXml(files)
        manifestXml  <- ImporterUtils.loadXml(manifestFile, ThirdPartyImportType.Qti)
      yield
        val result = validateZip(manifestXml, files, qtiImportType, taskReport, competenciesByName)
        if !result.taskReport.hasErrors then
          val manifestFile = ImporterUtils.writeManifestFile(result.manifest.get, mapper)
          ImporterUtils.writeExchangeZip(outputPath, manifestFile, FileLookups.lookup(files))
        result
      taskReport.markComplete()
      attempt.valueOr(bmNel =>
        bmNel.foreach(bm => taskReport.addError(bm))
        ExchangeReportDto(None, taskReport)
      )
    }

  private def validateZip(
    manifestXml: Elem,
    files: TempFileMap,
    qtiImportType: QtiImportType,
    taskReport: TaskReport,
    competenciesByName: Map[String, UUID],
  ): ExchangeReportDto =
    if qti2Importer.validateManifestSupported(manifestXml).isSuccess then
      qti2Importer.validateZip(manifestXml, files, qtiImportType, taskReport)
    else if qti1Importer.validateManifestSupported(manifestXml).isSuccess then
      qti1Importer.validateZip(manifestXml, files, qtiImportType, taskReport, competenciesByName)
    else
      taskReport.addError(AuthoringBundle.message("qti.import.notSupported"))
      ExchangeReportDto(None, taskReport)

  /** Writes a new manifest inside the zip that already exists at `exchangeZipPath` if there are changes to write based
    * on comparing `assessmentType` and `assessmentTitle` to values that are already in the manifest.
    *
    * @return
    *   the new manifest if the existing one was overwritten, None otherwise
    */
  def overwriteExchangeZip(
    exchangeZipPath: Path,
    assessmentType: String,
    assessmentTitle: Option[String] = None,
  ): Option[ExchangeManifest] =
    Using.resource(newTempFileMap(exchangeZipPath.getFileName.toString)) { files =>
      files.importZip(exchangeZipPath.toFile)
      val manifestFile = files.get("manifest.json")
      val manifest     = JacksonUtils.getFinatraMapper.readValue(manifestFile, classOf[ExchangeManifest])
      val rootNode     = manifest.nodes
        .find(n => rootQtiTypes.contains(n.typeId))
        .getOrElse(throw FatalQtiImportException(GuidUtil.errorGuid, "Failed to find root node in LO exchange ZIP"))
      val rootTypeId   = rootNode.typeId
      val rootTitle    = rootNode.data.get("title").asText()

      if assessmentType != rootTypeId then
        /* Type is different */
        val questionIds = rootNode.edges.filter(_.group == Questions).sortBy(_.position).map(_.target)
        val questions   = questionIds
          .map(id =>
            manifest.nodes
              .find(_.id == id)
              .getOrElse(
                throw FatalQtiImportException(GuidUtil.errorGuid(), s"Failed to find question $id in LO exchange ZIP")
              )
          )
        val newRoot     =
          assessmentBuilder.buildAssessmentOfType(assessmentType, assessmentTitle.getOrElse(rootTitle), questions)
        val newNodes    = newRoot +: manifest.nodes.filterNot(_.id == rootNode.id)
        val newManifest = manifest.copy(nodes = newNodes)
        ImporterUtils.overwriteManifestInExchangeZip(newManifest, exchangeZipPath, mapper)
        Some(newManifest)
      else if assessmentTitle.exists(_ != rootTitle) then
        /* Title is different */
        val newRootData = rootNode.data.asInstanceOf[ObjectNode].put("title", assessmentTitle.get)
        val newRoot     = rootNode.copy(data = newRootData)
        val newNodes    = newRoot +: manifest.nodes.filterNot(_.id == rootNode.id)
        val newManifest = manifest.copy(nodes = newNodes)
        ImporterUtils.overwriteManifestInExchangeZip(newManifest, exchangeZipPath, mapper)
        Some(newManifest)
      else
        /* Unchanged */
        None
      end if
    }

  private def newTempFileMap(name: String): TempFileMap =
    new TempFileMap(s"qti_${name}_${GuidUtil.shortGuid()}", ".tmp")
end QtiImporter

object QtiImporter:
  private val logger: Logger = org.log4s.getLogger
  private val IMSManifestXML = "imsmanifest.xml"
  private val rootQtiTypes   =
    Set(AssetTypeId.Assessment.entryName, AssetTypeId.PoolAssessment.entryName, AssetTypeId.Diagnostic.entryName)
