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

import java.io.FileInputStream

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.TempFileMap
import com.learningobjects.de.task.TaskReport
import loi.authoring.exchange.imprt.NodeFamily
import loi.authoring.exchange.imprt.exception.{
  AssessmentFileNotFoundException,
  AssessmentItemRefNotFoundException,
  InvalidAssessmentTestException
}
import loi.cp.i18n.AuthoringBundle
import org.log4s.Logger

import scala.xml.{Node, SAXParseException, XML}

@Service
class Qti2AssessmentImporter(assessmentBuilder: AssessmentBuilder):
  import Qti2AssessmentImporter.*

  def buildAssessment(
    manifest: Node,
    assessmentType: String,
    assessmentTitle: Option[String],
    convertedQuestions: Seq[ConvertedQuestion],
    files: TempFileMap,
    taskReport: TaskReport
  ): NodeFamily =
    val title      = assessmentTitle.getOrElse(getAssessmentTitle(manifest, files, taskReport))
    val questions  = getQuestions(manifest, convertedQuestions, files, taskReport)
    val assessment = assessmentBuilder.buildAssessmentOfType(assessmentType, title, questions.map(_.node))
    NodeFamily(assessment, questions.flatMap(_.family) ++ Seq(assessment))
  end buildAssessment

  /** Gets the assessment title from the QTI 2.x package. If a imsqti_test_xmlv2p1 resource is found, that title is
    * used. Else uses the QTI manifest title. If the manifest title is blank, "QTI Import" is used.
    */
  private def getAssessmentTitle(manifest: Node, files: TempFileMap, taskReport: TaskReport): String =
    val testResource = getTestResource(manifest)
    testResource
      .flatMap(r => getTestTitle(r, files, taskReport))
      .orElse(getManifestTitle(manifest))
      .getOrElse("QTI Import")

  private def getTestTitle(testResource: Node, files: TempFileMap, taskReport: TaskReport): Option[String] =
    val assessmentTest = getAssessmentTestXml(testResource, files, taskReport)
    val title          = (assessmentTest \ "@title").text.trim
    if title.isEmpty then None else Some(title)

  private def getAssessmentFileName(testResource: Node): String = (testResource \ "@href").text

  private def getAssessmentTestXml(testResource: Node, files: TempFileMap, taskReport: TaskReport): Node =
    val assessmentFileName = getAssessmentFileName(testResource)
    if !files.containsKey(assessmentFileName) then
      taskReport.addError(AuthoringBundle.message("qti.import.assessmentFileNotFound", assessmentFileName))
      throw AssessmentFileNotFoundException(assessmentFileName)
    val testFile           = files.get(assessmentFileName)
    try XML.load(new FileInputStream(testFile))
    catch
      case e: SAXParseException =>
        val msg = AuthoringBundle.message("qti.import.invalidAssessmentTest", assessmentFileName)
        logger.error(e)(msg.value)
        taskReport.addError(msg)
        throw InvalidAssessmentTestException(assessmentFileName)
  end getAssessmentTestXml

  private def getManifestTitle(manifest: Node): Option[String] =
    (manifest \ "metadata" \ "lom" \ "general" \ "title").headOption
      .map(_.text.trim)
      .flatMap(s => if s.isEmpty then None else Some(s))

  private def getQuestions(
    manifest: Node,
    convertedQuestions: Seq[ConvertedQuestion],
    files: TempFileMap,
    taskReport: TaskReport
  ): Seq[NodeFamily] =
    val testResource = getTestResource(manifest)
    testResource
      .map(r => getTestQuestions(convertedQuestions, r, files, taskReport))
      .getOrElse(convertedQuestions.map(cq => NodeFamily(cq.question, cq.family)))
  end getQuestions

  private def getTestResource(manifest: Node): Option[Node] =
    (manifest \ "resources" \ "resource").find(r => (r \ "@type").text == "imsqti_test_xmlv2p1")

  private def getTestQuestions(
    convertedQuestions: Seq[ConvertedQuestion],
    testResource: Node,
    files: TempFileMap,
    taskReport: TaskReport
  ): Seq[NodeFamily] =
    val assessmentFileName = getAssessmentFileName(testResource)
    val assessmentTest     = getAssessmentTestXml(testResource, files, taskReport)
    val questionMap        = convertedQuestions.map(cq => (cq.externalId, cq)).toMap
    val assessmentItemRefs = assessmentTest \ "testPart" \ "assessmentSection" \ "assessmentItemRef"
    val questionIdsInOrder = assessmentItemRefs.map(ref => (ref \ "@identifier").text)
    val testQuestions      = questionIdsInOrder.map { id =>
      questionMap.get(id) match
        case Some(cq) => NodeFamily(cq.question, cq.family)
        case None     =>
          taskReport.addError(AuthoringBundle.message("qti.import.assessmentItemRefNotFound", assessmentFileName, id))
          throw AssessmentItemRefNotFoundException(assessmentFileName, id)
    }
    val testQuestionIds    = testQuestions.map(_.node.id).toSet
    convertedQuestions.foreach { cq =>
      if !testQuestionIds.contains(cq.question.id) then
        /* Question was declared in the manifest but omitted from the test. */
        taskReport.addWarning(
          AuthoringBundle.message("qti.import.questionNotIncludedInTest", assessmentFileName, cq.externalId)
        )
    }
    testQuestions
  end getTestQuestions
end Qti2AssessmentImporter

object Qti2AssessmentImporter:
  private val logger: Logger = org.log4s.getLogger
