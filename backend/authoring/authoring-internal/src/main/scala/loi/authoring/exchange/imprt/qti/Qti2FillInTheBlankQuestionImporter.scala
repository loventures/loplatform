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

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.HtmlUtils
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.question.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.{buildEmptyEdgesFromAssets, guid}
import loi.authoring.exchange.imprt.exception.{FillInTheBlankInvalidBlankException, FillInTheBlankMissingBlankException}
import loi.authoring.exchange.imprt.qti.QtiImportUtils.{addWarningIfStylesheetIsPresent, getItemFeedback}
import loi.authoring.exchange.imprt.{ImporterUtils, NodeExchangeBuilder, NodeFamily}
import loi.cp.i18n.AuthoringBundle

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, Text}

@Service
class Qti2FillInTheBlankQuestionImporter(mapper: ObjectMapper):

  def buildQuestion(request: QuestionImportRequest): NodeFamily =
    val mathRequest                     = request.copy(xml = ImporterUtils.convertMath(request.xml))
    val (assessmentItem, images, edges) = Qti2ImageImporter.addImages(mathRequest)

    addWarningIfStylesheetIsPresent(assessmentItem, request.taskReport, request.fileName)

    val promptWithAnswer =
      if (assessmentItem \ "responseDeclaration" \ "correctResponse").isEmpty then
        getQuestionTextFromResponseProcessing(assessmentItem, request)
      else getQuestionTextFromResponseDeclation(assessmentItem, request)

    val (correctFeedback, incorrectFeedback) = getItemFeedback(assessmentItem)

    val fillInTheBlankData = FillInTheBlankQuestion(
      title = promptWithAnswer,
      keywords = null,
      questionContent = FillInTheBlankContent(
        questionTitle = Option(promptWithAnswer),
        questionText = Option(promptWithAnswer),
        allowDistractorRandomization = Some(true),
        questionComplexText = HtmlPart(promptWithAnswer),
        questionContentBlockText = BlockPart(parts = Seq(HtmlPart(promptWithAnswer))),
        richCorrectAnswerFeedback = Some(HtmlPart(correctFeedback)),
        richIncorrectAnswerFeedback = Some(HtmlPart(incorrectFeedback))
      )
    )

    val question = NodeExchangeBuilder
      .builder(guid, AssetTypeId.FillInTheBlankQuestion.entryName, mapper.valueToTree(fillInTheBlankData))
      .edges(buildEmptyEdgesFromAssets(images, Group.Resources, edges))
      .build()
    NodeFamily(question, Seq(question) ++ images)
  end buildQuestion

  private def getQuestionTextFromResponseDeclation(assessmentItem: Node, request: QuestionImportRequest): String =
    val answer = (assessmentItem \ "responseDeclaration" \ "correctResponse" \ "value").text.trim

    if answer.isEmpty then
      request.taskReport.addError(AuthoringBundle.message("qti.import.fillInTheBlank.invalidBlank", request.fileName))
      throw FillInTheBlankInvalidBlankException(request.fileName)

    /* Objects defined here so they can use `answer` */
    object TextEntryInteractionRewriteRule extends RewriteRule:
      override def transform(n: Node): Seq[Node] =
        n match
          case tei: Elem if tei.label == "textEntryInteraction" => Text(s"{{$answer}}")
          case _                                                => n

    object TextEntryInteractionRewriter extends RuleTransformer(TextEntryInteractionRewriteRule)

    val itemBody = (assessmentItem \ "itemBody").head

    if (itemBody \\ "textEntryInteraction").isEmpty then
      request.taskReport.addError(AuthoringBundle.message("qti.import.fillInTheBlank.missingBlank", request.fileName))
      throw FillInTheBlankMissingBlankException(request.fileName)

    val rewrite = TextEntryInteractionRewriter(itemBody)
    val clean   = ImporterUtils.cleanUpXml(rewrite.child.filterNot(_.label == "feedbackInline").mkString("\n"))
    /* HTML not supported. Convert to plaintext. */
    HtmlUtils.toPlaintext(clean)
  end getQuestionTextFromResponseDeclation

  private def getQuestionTextFromResponseProcessing(assessmentItem: Node, request: QuestionImportRequest): String =
    val answer =
      (assessmentItem \ "responseProcessing" \ "responseCondition" \ "responseIf" \ "or" \ "stringMatch" \ "baseValue")
        .map(a => (a.text.trim))
        .mkString(";")

    if answer.isEmpty then
      request.taskReport.addError(AuthoringBundle.message("qti.import.fillInTheBlank.invalidBlank", request.fileName))
      throw FillInTheBlankInvalidBlankException(request.fileName)

    val itemBody = (assessmentItem \\ "itemBody")
    val prompt   = (itemBody \ "textEntryInteraction" \ "prompt").text.trim
    val blank    = """_{3,}""".r
    blank.replaceAllIn(prompt, s"""{{$answer}}""")
  end getQuestionTextFromResponseProcessing
end Qti2FillInTheBlankQuestionImporter
