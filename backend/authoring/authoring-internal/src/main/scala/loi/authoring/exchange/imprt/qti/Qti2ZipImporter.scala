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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.TempFileMap
import com.learningobjects.de.task.TaskReport
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.exchange.imprt.qti.QtiImportUtils.*
import loi.authoring.exchange.imprt.web.{ExchangeReportDto, QtiImportType}
import loi.authoring.exchange.imprt.{ImportError, ImporterUtils}
import loi.authoring.exchange.model.ExchangeManifest
import loi.cp.i18n.BundleMessage
import scalaz.ValidationNel
import scalaz.syntax.`validation`.*
import scaloi.syntax.BooleanOps.*

import scala.xml.Elem

/** Imports QTI 2.x ZIP files.
  */
@Service
class Qti2ZipImporter(
  assessmentImporter: Qti2AssessmentImporter,
  questionImporter: Qti2QuestionImporter,
):

  def validateManifestSupported(manifest: Elem): ValidationNel[BundleMessage, Unit] =
    if (manifest \ "metadata" \ "schema").text == "QTIv2.1 Package" then ().successNel
    else
      val resources = manifest \ "resources" \ "resource"
      resources
        .exists(r =>
          val rtype = (r \ "@type").text
          rtype == "imsqti_test_xmlv2p1" || rtype == "imsqti_item_xmlv2p1"
        )
        .elseInvalidNel(ImportError.QtiImportNotSupported)

  def validateZip(
    manifestXml: Elem,
    files: TempFileMap,
    qtiImportType: QtiImportType,
    taskReport: TaskReport = ImporterUtils.createValidatingReport
  ): ExchangeReportDto =
    val attempt = validateManifestSupported(manifestXml).map(_ =>
      val convertedQuestions = getResources(manifestXml)
        .flatMap(r => questionImporter.buildQuestion(r, files, taskReport))

      // we always build the assessment, even if just importing plain questions
      // because sometimes more question data comes from a companion
      // imsqti_test file and we use it if we see it. The interpretation of the
      // imsqti_test file is in the assessmentImporter and I'm not refactoring it.
      // It is easier to just discard the assessment in our model if doing a
      // plain questions import.
      val assessmentType = AssetTypeId.Assessment.entryName
      val allNodes       = assessmentImporter
        .buildAssessment(
          manifestXml,
          assessmentType,
          None,
          convertedQuestions,
          files,
          taskReport
        )
        .family

      val nodes =
        if qtiImportType == QtiImportType.PlainQuestions then allNodes.filterNot(_.typeId == assessmentType)
        else allNodes

      val manifest = ExchangeManifest.empty.copy(nodes = nodes)
      ExchangeReportDto(Some(manifest), taskReport)
    )
    taskReport.markComplete()
    attempt.valueOr(bmNel =>
      bmNel.foreach(bm => taskReport.addError(bm))
      ExchangeReportDto(None, taskReport)
    )
  end validateZip
end Qti2ZipImporter
