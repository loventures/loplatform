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

import java.io.File
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.TempFileMap
import com.learningobjects.de.task.TaskReport
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.exchange.imprt.qti.QtiImportUtils.*
import loi.authoring.exchange.imprt.web.{ExchangeReportDto, QtiImportType}
import loi.authoring.exchange.imprt.{ImportError, ImporterUtils}
import loi.authoring.exchange.model.ExchangeManifest
import loi.cp.i18n.BundleMessage
import scalaz.syntax.applicative.*
import scalaz.{Validation, ValidationNel}
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.OptionOps.*

import java.util.UUID
import scala.xml.*

/** Imports QTI 1.x ZIP files.
  */
@Service
class Qti1ZipImporter(assessmentImporter: Qti1AssessmentImporter):
  import Qti1ZipImporter.*

  def validateManifestSupported(manifest: Elem): ValidationNel[BundleMessage, Unit] =
    getResources(manifest)
      .exists(r => supportedResourceTypes.contains((r \ "@type").text))
      .elseInvalidNel(ImportError.QtiImportNotSupported)

  def validateZip(
    manifestXml: Elem,
    files: TempFileMap,
    qtiImportType: QtiImportType,
    taskReport: TaskReport = ImporterUtils.createValidatingReport,
    competenciesByName: Map[String, UUID] = Map.empty,
  ): ExchangeReportDto =
    import Validation.FlatMap.*
    val attempt = for
      _                 <- validateManifestSupported(manifestXml)
      assessmentResource =
        getResources(manifestXml).filter(r => supportedResourceTypes.contains((r \ "@type").text)).head
      title              = getAssessmentTitle(manifestXml, assessmentResource)
      assessmentFile    <- getAssessmentFile(assessmentResource, files, taskReport)
    yield
      val assessmentFileName = getAssessmentFileName(assessmentResource)
      // assessmentType can be changed in 2nd request of qti import
      val assessmentType     = AssetTypeId.Assessment.entryName
      val assessment         =
        assessmentImporter.buildAssessment(
          title,
          assessmentFile,
          assessmentFileName,
          files,
          assessmentType,
          taskReport,
          competenciesByName = competenciesByName
        )

      val allNodes = assessment.family
      val nodes    =
        if qtiImportType == QtiImportType.PlainQuestions then allNodes.filterNot(_.typeId == assessmentType)
        else allNodes

      val manifest = ExchangeManifest.empty.copy(nodes = nodes)
      taskReport.markComplete()
      ExchangeReportDto(Some(manifest), taskReport)
    attempt.valueOr(bmNel =>
      bmNel.foreach(bm => taskReport.addError(bm))
      ExchangeReportDto(None, taskReport)
    )
  end validateZip

  private def getAssessmentTitle(manifest: Node, assessmentResource: Node): String =
    val resourceTitle = (assessmentResource \ "metadata" \ "lom" \ "general" \ "title").text.trim
    val manifestTitle = (manifest \ "metadata" \ "lom" \ "general" \ "title").text.trim
    if resourceTitle.nonEmpty then resourceTitle else if manifestTitle.nonEmpty then manifestTitle else "QTI Import"

  private def getAssessmentFileName(assessmentResource: Node): String = (assessmentResource \ "file" \ "@href").text

  private def getAssessmentFile(
    assessmentResource: Node,
    files: TempFileMap,
    taskReport: TaskReport
  ): ValidationNel[BundleMessage, File] =
    val assessmentFileName = getAssessmentFileName(assessmentResource)

    def validateFileNameDefined: ValidationNel[BundleMessage, Unit] =
      assessmentFileName.isEmpty.thenInvalidNel(ImportError.QtiAssessmentFileNotDefined)
    def validateFileExists: ValidationNel[BundleMessage, File]      =
      Option(files.get(assessmentFileName)).elseInvalidNel(ImportError.QtiAssessmentFileNotFound(assessmentFileName))
    validateFileNameDefined *> validateFileExists
  end getAssessmentFile
end Qti1ZipImporter

object Qti1ZipImporter:
  val supportedResourceTypes = Set("imsqti_xmlv1p2", "ims_qtiasiv1p2")
