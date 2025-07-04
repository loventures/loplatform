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
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.question.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.*
import loi.authoring.exchange.imprt.qti.QtiImportUtils.{addWarningIfStylesheetIsPresent, getItemFeedback}
import loi.authoring.exchange.imprt.{ImporterUtils, NodeExchangeBuilder, NodeFamily}

import scala.xml.Node

@Service
class Qti2OrderingQuestionImporter(mapper: ObjectMapper):

  def buildQuestion(request: QuestionImportRequest): NodeFamily =
    val mathRequest                     = request.copy(xml = ImporterUtils.convertMath(request.xml))
    val (assessmentItem, images, edges) = Qti2ImageImporter.addImages(mathRequest)

    addWarningIfStylesheetIsPresent(assessmentItem, request.taskReport, request.fileName)

    val questionPromptNode = (assessmentItem \ "itemBody" \ "orderInteraction" \ "prompt").head
    val questionText       = cleanupXml(questionPromptNode)

    val correctOrdering = (assessmentItem \ "responseDeclaration" \ "correctResponse" \ "value").map(_.text)
    val choiceMap       = (assessmentItem \ "itemBody" \ "orderInteraction" \ "simpleChoice")
      .map(c => c.attribute("identifier").get.text -> c.text)
      .toMap

    val orderedChoices = correctOrdering.zipWithIndex.map({ case (choice, idx) =>
      OrderingChoice(
        choiceContent = Some(HtmlPart(choiceMap(choice))),
        index = 0,
        correct = false,
        points = 0.0,
        correctChoiceFeedback = null,
        incorrectChoiceFeedback = null,
        answerIndex = idx,
        renderingIndex = 0,
        choiceIdentifier = null
      )
    })

    val (correctFeedback, incorrectFeedback) = getItemFeedback(assessmentItem)

    val questionContent = OrderingContent(
      questionTitle = None,
      questionText = Option(questionText),
      questionComplexText = HtmlPart(questionText),
      richCorrectAnswerFeedback = Some(HtmlPart(correctFeedback)),
      richIncorrectAnswerFeedback = Some(HtmlPart(incorrectFeedback)),
      choices = orderedChoices,
      questionContentBlockText =
        BlockPart(Seq(HtmlPart(renderedHtml = Some(questionText))), renderedHtml = Some(questionText))
    )

    val data = OrderingQuestion(
      title = questionText,
      questionContent = questionContent
    )

    val question = NodeExchangeBuilder
      .builder(guid, AssetTypeId.OrderingQuestion.entryName, mapper.valueToTree(data))
      .edges(buildEmptyEdgesFromAssets(images, Group.Resources, edges))
      .build()
    NodeFamily(question, Seq(question) ++ images)
  end buildQuestion

  private def cleanupXml(node: Node): String =
    ImporterUtils.cleanUpXml(node.child.filterNot(_.label == "feedbackInline").mkString("\n"))
end Qti2OrderingQuestionImporter
