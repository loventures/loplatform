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
import loi.authoring.exchange.imprt.exception.{InvalidAssessmentItemException, QuestionFileNotFoundException}
import loi.cp.i18n.AuthoringBundle
import org.log4s.Logger

import scala.xml.{Elem, Node, SAXParseException, XML}

/** Imports QTI 2.x question XML.
  */
@Service
class Qti2QuestionImporter(
  choiceQuestionImporter: Qti2ChoiceQuestionImporter,
  essayQuestionImporter: Qti2EssayQuestionImporter,
  fillInTheBlankQuestionImporter: Qti2FillInTheBlankQuestionImporter,
  matchingQuestionImporter: Qti2MatchingQuestionImporter,
  orderingQuestionImporter: Qti2OrderingQuestionImporter
):
  import Qti2QuestionImporter.*

  def buildQuestion(resource: Node, files: TempFileMap, taskReport: TaskReport): Option[ConvertedQuestion] =
    if isSupported(resource) && isSupportedInteractionType(resource, taskReport) then
      val questionFileName = getQuestionFileName(resource)
      val workingDirectory = getWorkingDirectory(questionFileName)
      val assessmentItem   = getQuestionXml(questionFileName, files, taskReport)
      val questionId       = getQuestionId(assessmentItem, questionFileName, taskReport)
      val request          = QuestionImportRequest(assessmentItem, questionFileName, workingDirectory, files, taskReport)
      val nodeFamily       = getInteractionType(resource) match
        case "choiceInteraction"       => choiceQuestionImporter.buildQuestion(request)
        case "extendedTextInteraction" => essayQuestionImporter.buildQuestion(request)
        case "orderInteraction"        => orderingQuestionImporter.buildQuestion(request)
        case "matchInteraction"        => matchingQuestionImporter.buildQuestion(request)
        case "textEntryInteraction"    => fillInTheBlankQuestionImporter.buildQuestion(request)
      Some(ConvertedQuestion(questionId, nodeFamily.node, nodeFamily.family))
    else None

  private def isSupported(resource: Node): Boolean =
    (resource \ "metadata" \ "schemaversion").text == "2.1" || (resource \ "@type").text == "imsqti_item_xmlv2p1"

  private def isSupportedInteractionType(r: Node, taskReport: TaskReport): Boolean =
    val isSupported = supportedInteractionTypes.contains(getInteractionType(r))
    if !isSupported then
      val identifier      = (r \ "@identifier").text
      val interactionType = getInteractionType(r)
      taskReport.addWarning(
        AuthoringBundle.message("qti.import.unsupportedInteractionType", identifier, interactionType)
      )
    isSupported

  private def getInteractionType(resource: Node): String =
    val lomType = (resource \ "metadata" \ "lom" \ "qtiMetadata" \ "interactionType").text
    if lomType.isEmpty then (resource \ "metadata" \ "qtiMetadata" \ "interactionType").text else lomType

  private def getQuestionFileName(resource: Node): String = (resource \ "@href").text

  private def getQuestionXml(fileName: String, files: TempFileMap, taskReport: TaskReport): Elem =
    if !files.containsKey(fileName) then
      taskReport.addError(AuthoringBundle.message("qti.import.questionFileNotFound", fileName))
      throw QuestionFileNotFoundException(fileName)
    val questionFile = files.get(fileName)
    try XML.load(new FileInputStream(questionFile))
    catch
      case e: SAXParseException =>
        val msg = AuthoringBundle.message("qti.import.invalidAssessmentItem", fileName)
        logger.error(e)(msg.value)
        taskReport.addError(msg)
        throw InvalidAssessmentItemException(fileName)
  end getQuestionXml

  private def getWorkingDirectory(fileName: String): String =
    val end = fileName.lastIndexOf("/")
    if end < 0 then "" else fileName.substring(0, end)

  private def getQuestionId(assessmentItem: Node, questionFileName: String, taskReport: TaskReport): String =
    val id = (assessmentItem \ "@identifier").text
    if id.isEmpty then
      taskReport.addError(AuthoringBundle.message("qti.import.invalidAssessmentItem", questionFileName))
      throw InvalidAssessmentItemException(questionFileName)
    id
end Qti2QuestionImporter

object Qti2QuestionImporter:
  private val logger: Logger = org.log4s.getLogger

  private val supportedInteractionTypes = Set(
    "choiceInteraction",
    "extendedTextInteraction",
    "orderInteraction",
    "matchInteraction",
    "textEntryInteraction"
  )
end Qti2QuestionImporter
