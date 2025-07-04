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
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.TempFileMap
import com.learningobjects.de.task.{TaskReport, UnboundedTaskReport}
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.exchange.imprt.NodeFamily
import loi.authoring.exchange.imprt.exception.{InvalidQti1Exception, UnsupportedQuestionTypeException}
import loi.authoring.exchange.model.NodeExchangeData
import loi.cp.i18n.AuthoringBundle

import java.util.UUID
import scala.xml.{SAXParseException, XML}

/** Imports QTI 1.x assessments.
  */
@Service
class Qti1AssessmentImporter(
  assessmentBuilder: AssessmentBuilder,
  questionImporter: Qti1QuestionImporter,
):

  def buildAssessment(
    title: String,
    assessmentFile: File,
    assessmentFileName: String,
    files: TempFileMap,
    assessmentType: String = AssetTypeId.Assessment.entryName,
    taskReport: TaskReport = new UnboundedTaskReport("QTI Import"),
    imsccFilebase: String = "",
    competenciesByName: Map[String, UUID] = Map.empty,
  ): NodeFamily =
    val (questions, questionNodes) =
      buildQuestions(assessmentFile, assessmentFileName, files, taskReport, imsccFilebase, competenciesByName)
    val assessment                 = assessmentBuilder.buildAssessmentOfType(assessmentType, title, questions)
    NodeFamily(assessment, questionNodes ++ Seq(assessment))
  end buildAssessment

  /* Returns tuple of (question nodes, question nodes and their dependent nodes) */
  private def buildQuestions(
    assessmentFile: File,
    assessmentFileName: String,
    files: TempFileMap,
    taskReport: TaskReport,
    imsccFilebase: String,
    competenciesByName: Map[String, UUID],
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
    val assessmentXml = new FileInputStream(assessmentFile)
    try
      val questestinterop = XML.load(assessmentXml)

      /* We don't have sections so just extract all questions in order. */
      val items                = questestinterop \ "assessment" \ "section" \\ "item"
      val questionNodeFamilies = items.flatMap(item =>
        try
          val request =
            QuestionImportRequest(item, assessmentFileName, workingDirectory = "", files, taskReport, imsccFilebase)
          Some(questionImporter.buildQuestion(request, competenciesByName))
        catch
          /* Ignore unsupported question types. */
          case e: UnsupportedQuestionTypeException => None
      )
      (questionNodeFamilies.map(_.node), questionNodeFamilies.flatMap(_.family))
    catch
      case e: SAXParseException =>
        taskReport.addError(AuthoringBundle.message("qti.import.invalidQti1", assessmentFile.getName))
        throw InvalidQti1Exception(assessmentFile.getName)
    end try
  end buildQuestions
end Qti1AssessmentImporter
