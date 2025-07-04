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
import loi.authoring.exchange.imprt.exception.MatchingChoiceNotFoundException
import loi.authoring.exchange.imprt.qti.QtiImportUtils.{addWarningIfStylesheetIsPresent, getItemFeedback}
import loi.authoring.exchange.imprt.{ImporterUtils, NodeExchangeBuilder, NodeFamily}
import loi.authoring.exchange.model.{EdgeExchangeData, NodeExchangeData}
import loi.cp.i18n.AuthoringBundle

import scala.xml.{Node, NodeSeq}

@Service
class Qti2MatchingQuestionImporter(mapper: ObjectMapper):

  def buildQuestion(request: QuestionImportRequest): NodeFamily =
    val mathRequest                       = request.copy(xml = ImporterUtils.convertMath(request.xml))
    val (assessmentItem, images, edgeMap) = Qti2ImageImporter.addImages(mathRequest)

    addWarningIfStylesheetIsPresent(assessmentItem, request.taskReport, request.fileName)

    val matchInteraction = (assessmentItem \ "itemBody" \ "matchInteraction").head
    val prompt           = cleanupXml((matchInteraction \ "prompt").head)

    val correctResponse = (assessmentItem \ "responseDeclaration" \ "correctResponse" \ "value")
      .map(n =>
        val value   = n.text
        val pair    = value.split(" ")
        val leftId  = pair(0)
        val rightId = pair(1)
        leftId -> rightId
      )
      .toMap

    val (lefts, rights) = buildAssociations(matchInteraction, correctResponse, request)

    val edges = buildEmptyEdgesFromAssets(images, Group.Resources, edgeMap)

    val (correctFeedback, incorrectFeedback) = getItemFeedback(assessmentItem)

    val question =
      if isMatchingQuestion(matchInteraction, correctResponse) then
        buildMatchingQuestion(prompt, lefts, rights, edges, correctFeedback, incorrectFeedback)
      else buildBinDropQuestion(prompt, lefts, rights, edges, correctFeedback, incorrectFeedback)

    NodeFamily(question, Seq(question) ++ images)
  end buildQuestion

  private def isMatchingQuestion(matchInteraction: Node, correctResponse: Map[String, String]): Boolean =
    val maxAssociations = matchInteraction.attribute("maxAssociations").get.text.toInt
    val leftChoices     = getLeftChoices(matchInteraction)
    val rightChoices    = getRightChoices(matchInteraction)
    val numLeft         = leftChoices.size
    val numRight        = rightChoices.size
    /* Check that a 1-1 mapping is possible. */
    if (maxAssociations == correctResponse.size) && (maxAssociations == numLeft) && (numLeft == numRight) then
      val leftIds  = getChoiceIds(leftChoices)
      val rightIds = getChoiceIds(rightChoices)
      /* Check that a 1-1 mapping exists. */
      (correctResponse.keySet == leftIds.toSet) && (correctResponse.values.toSet == rightIds.toSet)
    else false
  end isMatchingQuestion

  private def buildAssociations(
    matchInteraction: Node,
    correctResponse: Map[String, String],
    request: QuestionImportRequest
  ): (Seq[Left], Seq[Right]) =
    val choiceNodes = matchInteraction \ "simpleMatchSet" \ "simpleAssociableChoice"
    val choiceMap   = choiceNodes.map(n => (n \ "@identifier").text -> cleanupXml(n)).toMap

    /* Every correct choice ID should have a corresponding label */
    val correctChoiceIds = correctResponse.keySet ++ correctResponse.values.toSet
    correctChoiceIds.foreach { choiceId =>
      if !choiceMap.contains(choiceId) then
        request.taskReport.addError(
          AuthoringBundle.message("qti.import.matching.choiceNotFound", request.fileName, choiceId)
        )
        throw MatchingChoiceNotFoundException(request.fileName, choiceId)
    }

    val rightChoices  = getRightChoices(matchInteraction)
    val rightIds      = getChoiceIds(rightChoices)
    val indexedRight  = rightIds.map(id => choiceMap(id)).zipWithIndex
    val rightIndexMap = indexedRight.map { case (label, index) => label -> index.toLong }.toMap
    val rights        = indexedRight.map { case (label, index) => Right(HtmlUtils.toPlaintext(label), index) }

    val leftChoices = getLeftChoices(matchInteraction)
    val leftIds     = getChoiceIds(leftChoices)
    val lefts       = leftIds.zipWithIndex.map { case (id, index) =>
      val rightId    = correctResponse.get(id)
      val rightLabel = rightId.map(id => choiceMap(id))
      val rightIndex = rightLabel.map(label => rightIndexMap(label))
      val leftLabel  = choiceMap(id)
      Left(HtmlUtils.toPlaintext(leftLabel), index, rightIndex)
    }

    (lefts, rights)
  end buildAssociations

  private def buildBinDropQuestion(
    prompt: String,
    lefts: Seq[Left],
    rights: Seq[Right],
    edges: Seq[EdgeExchangeData],
    correctFeedback: String,
    incorrectFeedback: String
  ): NodeExchangeData =
    val options = lefts.map(left => OptionContent(left.leftIndex, left.label, left.rightIndex))
    val bins    = rights.map(right => BinContent(right.index, right.label))
    val data    = BinDropQuestion(
      title = prompt,
      questionContent = BinDropContent(
        questionTitle = Option(prompt),
        questionText = Option(prompt),
        questionComplexText = HtmlPart(prompt),
        questionContentBlockText = BlockPart(parts = Seq(HtmlPart(prompt))),
        richCorrectAnswerFeedback = Some(HtmlPart(correctFeedback)),
        richIncorrectAnswerFeedback = Some(HtmlPart(incorrectFeedback)),
        bins = bins,
        options = options
      )
    )
    NodeExchangeBuilder
      .builder(guid, AssetTypeId.BinDropQuestion.entryName, mapper.valueToTree(data))
      .edges(edges)
      .build()
  end buildBinDropQuestion

  private def buildMatchingQuestion(
    prompt: String,
    lefts: Seq[Left],
    rights: Seq[Right],
    edges: Seq[EdgeExchangeData],
    correctFeedback: String,
    incorrectFeedback: String
  ): NodeExchangeData =
    val terms       = lefts.map(left =>
      TermContent(
        termIdentifier = Option(left.leftIndex.toString),
        termText = left.label,
        pointValue = null,
        feedbackInline = None,
        index = Option(left.leftIndex),
        correctIndex = left.rightIndex.get
      )
    )
    val definitions = rights.map(right =>
      DefinitionContent(
        definitionText = right.label,
        index = right.index
      )
    )
    val data        = MatchingQuestion(
      title = prompt,
      questionContent = MatchingContent(
        questionTitle = Option(prompt),
        questionText = Option(prompt),
        allowDistractorRandomization = Some(true),
        questionComplexText = HtmlPart(prompt),
        questionContentBlockText = BlockPart(parts = Seq(HtmlPart(prompt))),
        richCorrectAnswerFeedback = Some(HtmlPart(correctFeedback)),
        richIncorrectAnswerFeedback = Some(HtmlPart(incorrectFeedback)),
        multipleDefinitionsPerTermAllowed = false,
        terms = terms,
        definitionContent = definitions
      )
    )
    NodeExchangeBuilder
      .builder(guid, AssetTypeId.MatchingQuestion.entryName, mapper.valueToTree(data))
      .edges(edges)
      .build()
  end buildMatchingQuestion

  private def getLeftChoices(matchInteraction: Node): NodeSeq =
    (matchInteraction \ "simpleMatchSet").head \ "simpleAssociableChoice"

  private def getRightChoices(matchInteraction: Node): NodeSeq =
    (matchInteraction \ "simpleMatchSet")(1) \ "simpleAssociableChoice"

  private def getChoiceIds(choiceNodes: NodeSeq): Seq[String] = choiceNodes.map(c => (c \ "@identifier").text)

  private def cleanupXml(node: Node): String =
    ImporterUtils.cleanUpXml(node.child.filterNot(_.label == "feedbackInline").mkString("\n"))

  case class Left(label: String, leftIndex: Long, rightIndex: Option[Long])
  case class Right(label: String, index: Long)
end Qti2MatchingQuestionImporter
