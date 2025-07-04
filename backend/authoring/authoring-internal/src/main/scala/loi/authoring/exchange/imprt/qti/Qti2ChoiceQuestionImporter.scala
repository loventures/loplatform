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
import loi.asset.contentpart.HtmlPart
import loi.asset.question.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.*
import loi.authoring.exchange.imprt.exception.{
  ChoiceNotFoundException,
  MissingCorrectChoiceException,
  UnsupportedAssetTypeException
}
import loi.authoring.exchange.imprt.qti.QtiImportUtils.*
import loi.authoring.exchange.imprt.{ImporterUtils, NodeExchangeBuilder, NodeFamily}
import loi.cp.i18n.AuthoringBundle

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq}

/** Imports QTI 2.x choice questions (multiple choice, multiple select, and true/false).
  */
@Service
class Qti2ChoiceQuestionImporter(mapper: ObjectMapper):
  import Qti2ChoiceQuestionImporter.*

  def buildQuestion(request: QuestionImportRequest): NodeFamily =
    val mathRequest                     = request.copy(xml = ImporterUtils.convertMath(request.xml))
    val (assessmentItem, images, edges) = Qti2ImageImporter.addImages(mathRequest)

    addWarningIfStylesheetIsPresent(assessmentItem, request.taskReport, request.fileName)

    val typeId                       = getTypeIdForQuestion(assessmentItem)
    val shuffle                      = (assessmentItem \ "itemBody" \ "choiceInteraction" \ "@shuffle").text.toBoolean
    val allowDistractorRandomization = shuffle && typeId != TrueFalseType

    val questionText         = getQuestionText((assessmentItem \ "itemBody").head)
    val questionTextHtmlPart = HtmlPart(questionText)

    val choices = buildChoices(typeId, assessmentItem, request)

    val (correctFeedback, incorrectFeedback) = getItemFeedback(assessmentItem)

    val questionContent = ChoiceQuestionContent(
      allowDistractorRandomization = Some(allowDistractorRandomization),
      questionComplexText = questionTextHtmlPart,
      richCorrectAnswerFeedback = Some(HtmlPart(correctFeedback)),
      richIncorrectAnswerFeedback = Some(HtmlPart(incorrectFeedback)),
      choiceListingType = null,
      choices = choices,
    )

    val data = typeId match
      case AssetTypeId.TrueFalseQuestion      =>
        TrueFalseQuestion(
          questionContent = questionContent,
          title = "Untitled"
        )
      case AssetTypeId.MultipleChoiceQuestion =>
        MultipleChoiceQuestion(
          questionContent = questionContent,
          title = "Untitled"
        )
      case AssetTypeId.MultipleSelectQuestion =>
        MultipleSelectQuestion(
          questionContent = questionContent,
          title = "Untitled"
        )
      case _                                  => throw UnsupportedAssetTypeException(typeId)

    val question = NodeExchangeBuilder
      .builder(guid, typeId.entryName, mapper.valueToTree(data))
      .edges(buildEmptyEdgesFromAssets(images, Group.Resources, edges))
      .build()
    NodeFamily(question, Seq(question) ++ images)
  end buildQuestion

  private def getQuestionText(itemBody: Node): String =
    val body   = itemBody.child.filter(_.label != "choiceInteraction").mkString
    val prompt = "<p>" + (itemBody \ "choiceInteraction" \ "prompt").head.child.mkString + "</p>"
    ImporterUtils.cleanUpXml(body + prompt)

  private def buildChoices(
    typeId: AssetTypeId,
    assessmentItem: Node,
    request: QuestionImportRequest
  ): Seq[ChoiceContent] =
    val correctResponses = assessmentItem \ "responseDeclaration" \ "correctResponse"
    val correctChoiceIds = (correctResponses \ "value").map(_.text)

    if correctChoiceIds.isEmpty then
      request.taskReport.addError(AuthoringBundle.message("qti.import.correctChoiceNotSpecified", request.fileName))
      throw MissingCorrectChoiceException(request.fileName)

    val simpleChoices        = assessmentItem \ "itemBody" \ "choiceInteraction" \ "simpleChoice"
    val simpleChoiceMappings = simpleChoices.map(c => c.attribute("identifier").get.text -> c)

    correctChoiceIds.foreach { choiceId =>
      if !simpleChoiceMappings.toMap.contains(choiceId) then
        request.taskReport.addError(
          AuthoringBundle.message("qti.import.choice.choiceNotFound", request.fileName, choiceId)
        )
        throw ChoiceNotFoundException(request.fileName, choiceId)
    }

    typeId match
      case TrueFalseType            => buildTrueFalseChoices(simpleChoiceMappings, correctChoiceIds.head)
      case MultipleChoiceType       => buildMultipleChoices(simpleChoiceMappings, correctChoiceIds)
      case MultipleSelectChoiceType => buildMultipleSelectChoices(simpleChoiceMappings, correctChoiceIds)
      case t                        =>
        request.taskReport.addError(AuthoringBundle.message("qti.import.unsupportedAssetType", t.entryName))
        throw UnsupportedAssetTypeException(t)
  end buildChoices

  private def buildTrueFalseChoices(
    simpleChoiceMappings: Seq[(String, Node)],
    correctChoiceId: String
  ): Seq[ChoiceContent] =
    simpleChoiceMappings.map({ case (id, node) => buildTrueFalseChoice(node, id == correctChoiceId) })

  private def buildTrueFalseChoice(simpleChoice: Node, isCorrect: Boolean): ChoiceContent =
    val value                                            = RemoveInlineFeedbackTransformer(simpleChoice).text.trim.equalsIgnoreCase("true")
    val content                                          = if value then "true" else "false"
    val index                                            = if value then 0 else 1
    val pointValue                                       = getPointValue(isCorrect)
    val (correctChoiceFeedback, incorrectChoiceFeedback) = getChoiceFeedback(simpleChoice, isCorrect)
    ChoiceContent(
      choiceContent = Some(HtmlPart(content)),
      index = index,
      correct = isCorrect,
      points = pointValue,
      correctChoiceFeedback = correctChoiceFeedback,
      incorrectChoiceFeedback = incorrectChoiceFeedback
    )
  end buildTrueFalseChoice

  private def buildMultipleChoices(
    simpleChoiceMappings: Seq[(String, Node)],
    correctChoiceIds: Seq[String]
  ): Seq[ChoiceContent] =
    simpleChoiceMappings.zipWithIndex.map { case ((id, node), index) =>
      val isCorrect                                        = correctChoiceIds.contains(id)
      val content                                          = cleanupChoiceXml(node)
      val (correctChoiceFeedback, incorrectChoiceFeedback) = getChoiceFeedback(node, isCorrect)
      val pointValue                                       = getPointValue(isCorrect)
      ChoiceContent(
        choiceContent = Some(HtmlPart(content)),
        index = index,
        correct = isCorrect,
        points = pointValue,
        correctChoiceFeedback = correctChoiceFeedback,
        incorrectChoiceFeedback = incorrectChoiceFeedback
      )
    }

  private def getPointValue(isCorrect: Boolean): Int = if isCorrect then 1 else 0

  private def buildMultipleSelectChoices(
    simpleChoiceMappings: Seq[(String, Node)],
    correctChoiceIds: Seq[String]
  ): Seq[ChoiceContent] =
    val numCorrect      = correctChoiceIds.size
    val correctPoints   = 1d / numCorrect
    val numIncorrect    = simpleChoiceMappings.size - numCorrect
    val incorrectPoints = -1d / numIncorrect
    simpleChoiceMappings.zipWithIndex.map { case ((id, node), index) =>
      val isCorrect                                        = correctChoiceIds.contains(id)
      val content                                          = cleanupChoiceXml(node)
      val (correctChoiceFeedback, incorrectChoiceFeedback) = getChoiceFeedback(node, isCorrect)
      val pointValue                                       = if isCorrect then correctPoints else incorrectPoints
      ChoiceContent(
        choiceContent = Some(HtmlPart(content)),
        index = index,
        correct = isCorrect,
        points = pointValue,
        correctChoiceFeedback = correctChoiceFeedback,
        incorrectChoiceFeedback = incorrectChoiceFeedback
      )
    }
  end buildMultipleSelectChoices

  private def getChoiceFeedback(simpleChoice: Node, isCorrect: Boolean): (Option[HtmlPart], Option[HtmlPart]) =
    val inlineFeedback          = (simpleChoice \ "feedbackInline").text
    val correctChoiceFeedback   = if isCorrect && !inlineFeedback.isEmpty then Some(HtmlPart(inlineFeedback)) else None
    val incorrectChoiceFeedback = if !isCorrect && !inlineFeedback.isEmpty then Some(HtmlPart(inlineFeedback)) else None
    (correctChoiceFeedback, incorrectChoiceFeedback)

  private def cleanupChoiceXml(node: Node): String =
    ImporterUtils.cleanUpXml(node.child.filterNot(_.label == "feedbackInline").mkString("\n")).trim

  private def getTypeIdForQuestion(assessmentItem: Node): AssetTypeId =
    val correctResponses = assessmentItem \ "responseDeclaration" \ "correctResponse" \ "value"
    if isTrueFalse(assessmentItem) then TrueFalseType
    else
      correctResponses.size match
        case 1 => MultipleChoiceType
        case _ => MultipleSelectChoiceType

  private def isTrueFalse(assessmentItem: Node): Boolean =
    /* QTI spec is not helpful here so I'm just checking that there's only 2 choices with values true and false. */
    val noInlineFeedback = RemoveInlineFeedbackTransformer(assessmentItem)
    val simpleChoices    = noInlineFeedback \ "itemBody" \ "choiceInteraction" \ "simpleChoice"
    simpleChoices.lengthCompare(2) == 0 &&
    simpleChoices.exists(_.text.trim.equalsIgnoreCase("true")) &&
    simpleChoices.exists(_.text.trim.equalsIgnoreCase("false"))
end Qti2ChoiceQuestionImporter

object Qti2ChoiceQuestionImporter:
  private val TrueFalseType            = AssetTypeId.TrueFalseQuestion
  private val MultipleChoiceType       = AssetTypeId.MultipleChoiceQuestion
  private val MultipleSelectChoiceType = AssetTypeId.MultipleSelectQuestion

object RemoveInlineFeedbackRule extends RewriteRule:
  override def transform(n: Node): Seq[Node] =
    n match
      case fi: Elem if fi.label == "feedbackInline" => NodeSeq.Empty
      case e                                        => e

object RemoveInlineFeedbackTransformer extends RuleTransformer(RemoveInlineFeedbackRule)
