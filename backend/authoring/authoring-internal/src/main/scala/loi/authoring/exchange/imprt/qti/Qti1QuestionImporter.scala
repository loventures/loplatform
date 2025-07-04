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
import com.learningobjects.de.task.TaskReport
import loi.asset.competency.service.CompetencyService
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.question.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.{buildEmptyEdgesFromAssets, guid}
import loi.authoring.exchange.imprt.exception.UnsupportedQuestionTypeException
import loi.authoring.exchange.imprt.{NodeExchangeBuilder, NodeFamily}
import loi.authoring.exchange.model.EdgeExchangeData
import loi.cp.asset.edge.EdgeData
import loi.cp.i18n.AuthoringBundle
import org.apache.commons.text.StringEscapeUtils
import scalaz.std.tuple.*
import scalaz.syntax.bifunctor.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.option.*

import java.util.UUID
import scala.xml.{Elem, Node, NodeSeq}

/** Imports QTI 1.x questions.
  */
@Service
class Qti1QuestionImporter(mapper: ObjectMapper):

  import Qti1QuestionImporter.*

  def buildQuestion(request: QuestionImportRequest, competenciesByName: Map[String, UUID] = Map.empty): NodeFamily =
    val xml            = request.xml
    val taskReport     = request.taskReport
    val questionId     = getQuestionId(xml)
    val questionTypeId = getTypeIdForQuestion(xml) | {
      taskReport.addWarning(AuthoringBundle.message("qti.import.unsupportedQuestionType", questionId, "Unknown"))
      throw UnsupportedQuestionTypeException(questionId, "Unknown")
    }

    val (item, images, edges) = Qti1ImageImporter.addImages(request)

    val builder = questionTypeId match
      case AssetTypeId.EssayQuestion => buildEssayQuestion(item)
      case other                     => buildChoiceQuestion(item, other)

    val question = builder
      .edges(
        buildEmptyEdgesFromAssets(images, Group.Resources, edges) ++
          buildCompetencyEdges(getCompetencies(xml, competenciesByName, taskReport))
      )
      .build()

    NodeFamily(question, Seq(question) ++ images)
  end buildQuestion

  private def buildCompetencyEdges(competencies: List[UUID]): List[EdgeExchangeData] =
    for (competency, position) <- competencies.zipWithIndex
    yield EdgeExchangeData(
      Group.Assesses,
      competency.toString,
      position,
      traverse = false,
      UUID.randomUUID(),
      EdgeData.empty,
      targetInWorkspace = true
    )

  private def getCompetencies(
    item: Node,
    competenciesByName: Map[String, UUID],
    taskReport: TaskReport
  ): List[UUID] =
    for
      field      <- CompetencyMetadataFields
      title      <- getMetadata(item, field)
      competency <-
        competenciesByName
          .get(CompetencyService.normalize(title))
          .tapNone(
            taskReport.addWarning(
              AuthoringBundle.message("qti.import.competency.notFound", getQuestionId(item), field, title)
            )
          )
    yield competency

  private def getTypeIdForQuestion(item: Node): Option[AssetTypeId] =
    /* Try to determine the question type via Canvas metadata. */
    val metadataQuestionType = getMetadata(item, "question_type") | ""
    PartialFunction.condOpt(metadataQuestionType) {
      case "true_false_question"       => AssetTypeId.TrueFalseQuestion
      case "multiple_choice_question"  => AssetTypeId.MultipleChoiceQuestion
      case "multiple_answers_question" => AssetTypeId.MultipleSelectQuestion
      case "essay_question"            => AssetTypeId.EssayQuestion
    } || {
      if isTrueFalse(item) then Some(AssetTypeId.TrueFalseQuestion)
      else if isMultipleChoice(item) then Some(AssetTypeId.MultipleChoiceQuestion)
      else if isMultipleSelect(item) then Some(AssetTypeId.MultipleSelectQuestion)
      else if isEssay(item) then Some(AssetTypeId.EssayQuestion)
      else None
    }
  end getTypeIdForQuestion

  private def getMetadata(item: Node, field: String): Option[String] =
    (item \ "itemmetadata" \ "qtimetadata" \ "qtimetadatafield")
      .find(n => (n \ "fieldlabel").text == field)
      .map(f => (f \ "fieldentry").text)

  private def isTrueFalse(item: Node): Boolean =
    if rcardinality(item) == "Single" then
      val choiceLabels = getChoiceLabelMap(item).values
      /* QTI 1.2 true/false examples exist with Agree/Disagree but those would be multiple choice in our system. */
      choiceLabels.size == 2 && choiceLabels.exists(l => l.equalsIgnoreCase("true")) &&
      choiceLabels.exists(l => l.equalsIgnoreCase("false"))
    else false

  private def rcardinality(item: Node): String =
    (item \ "presentation" \ "response_lid" \ "@rcardinality").text +
      (item \ "presentation" \ "flow" \ "response_lid" \ "@rcardinality").text

  private def isMultipleChoice(item: Node): Boolean =
    rcardinality(item) == "Single" && !isTrueFalse(item)

  private def isMultipleSelect(item: Node): Boolean =
    rcardinality(item) == "Multiple"

  private def isEssay(item: Node): Boolean =
    val isFillInTheBlank = ((item \ "presentation" \ "response_str" \ "render_fib") ++
      (item \ "presentation" \ "flow" \ "response_str" \ "render_fib")).nonEmpty
    val hasNoAnswer      = (item \ "resprocessing" \ "respcondition" \ "conditionvar" \ "varequal").isEmpty
    isFillInTheBlank && hasNoAnswer

  private def buildChoiceQuestion(item: Node, questionTypeId: AssetTypeId): NodeExchangeBuilder =
    val questionText = getQuestionText(item)

    val choices = questionTypeId match
      case AssetTypeId.TrueFalseQuestion => buildTrueFalseChoices(item)
      case other                         => buildMultipleChoices(item, other)

    // http://www.imsglobal.org/question/qtiv1p2/imsqti_litev1p2.html#1404425
    // shuffle (optional - enumerated list of: Yes, No. Default = No).
    // Shows whether or not the list of possible responses can be shuffled between consecutive displays to the user.
    // See also
    // response_label \ rshuffle (optional - enumerated list of: Yes, No. Default = Yes).
    // Defines whether the associated response_label can be shuffled between consecutive displays to the user.
    // Typically rshuffle="No" is used on the last "None of the above" choice.
    val shuffle              = (item \ "presentation" \ "flow" \ "response_lid" \ "render_choice" \ "@shuffle").text == "Yes"
    val randomizeDistractors = shuffle && questionTypeId != AssetTypeId.TrueFalseQuestion

    val feedbacks = getQuestionFeedback(item)

    val questionContent = ChoiceQuestionContent(
      allowDistractorRandomization = Some(randomizeDistractors),
      questionComplexText = HtmlPart(questionText),
      choices = choices,
      richCorrectAnswerFeedback = feedbacks.map(tuple => HtmlPart(tuple._1)),
      richIncorrectAnswerFeedback = feedbacks.map(tuple => HtmlPart(tuple._2))
    )

    val data = questionTypeId match
      case AssetTypeId.TrueFalseQuestion      =>
        TrueFalseQuestion(
          questionContent = questionContent,
          title = "Untitled", // filled in on commit
          keywords = null
        )
      case AssetTypeId.MultipleChoiceQuestion =>
        MultipleChoiceQuestion(
          questionContent = questionContent,
          title = "Untitled",
          keywords = null
        )
      case AssetTypeId.MultipleSelectQuestion =>
        MultipleSelectQuestion(
          questionContent = questionContent,
          title = "Untitled",
          keywords = null
        )
      case _                                  =>
        throw UnsupportedQuestionTypeException(getQuestionId(item), questionTypeId.entryName)

    NodeExchangeBuilder.builder(guid, questionTypeId.entryName, mapper.valueToTree(data))
  end buildChoiceQuestion

  private def buildEssayQuestion(item: Node): NodeExchangeBuilder =
    val questionText = getQuestionText(item)
    val data         = EssayQuestion(
      title = "Untitled",
      questionContent = EssayContent(
        questionContentBlockText = BlockPart(parts = Seq(HtmlPart(questionText)))
      ),
    )
    NodeExchangeBuilder.builder(guid, AssetTypeId.EssayQuestion.entryName, mapper.valueToTree(data))

  private def getQuestionId(item: Node): String = (item \ "@ident").headOption.map(_.text).getOrElse("Unknown")

  private def getQuestionText(item: Node): String =
    /* flow is optional. */
    val promptNodes =
      (item \ "presentation" \ "material" \ "mattext") ++ (item \ "presentation" \ "flow" \ "material" \ "mattext")
    htmlText(promptNodes)

  private def getChoiceIds(item: Node): Seq[String] =
    getChoiceResponses(item).map(n => (n \ "@ident").text)

  private def getChoiceResponses(item: Node): Seq[Node] =
    /* flow and flow_label are optional. */
    (item \ "presentation" \ "response_lid" \ "render_choice" \ "response_label") ++
      (item \ "presentation" \ "flow" \ "response_lid" \ "render_choice" \ "flow_label" \ "response_label")

  private def getChoiceLabelMap(item: Node): Map[String, String] =
    getChoiceResponses(item)
      .map(n =>
        /* flow_mat is optional */
        val labelNodes = (n \ "material" \ "mattext") ++ (n \ "flow_mat" \ "material" \ "mattext")
        (n \ "@ident").text -> htmlText(labelNodes)
      )
      .toMap

  private def buildTrueFalseChoices(item: Node): Seq[ChoiceContent] =
    val choiceLabelMap  = getChoiceLabelMap(item)
    val choicePointsMap = getMultipleChoicePointsMap(item)

    val choiceIds = getChoiceIds(item)
    choiceIds.map { id =>
      val label     = choiceLabelMap.getOrElse(id, "false")
      val value     = label.equalsIgnoreCase("true")
      val points    = choicePointsMap.getOrElse(id, 0d)
      val isCorrect = points == 1d
      val feedback  = getDistractorFeedback(item, id)
      buildTrueFalseChoice(value, isCorrect, feedback)
    }
  end buildTrueFalseChoices

  private def buildTrueFalseChoice(value: Boolean, isCorrect: Boolean, feedback: Option[String]): ChoiceContent =
    val content    = if value then "true" else "false"
    val index      = if value then 0 else 1
    val pointValue = getPointValue(isCorrect)
    ChoiceContent(
      choiceContent = Some(HtmlPart(content)),
      index = index,
      correct = isCorrect,
      points = pointValue,
      correctChoiceFeedback = feedback.when(isCorrect).map(HtmlPart(_)),
      incorrectChoiceFeedback = feedback.unless(isCorrect).map(HtmlPart(_)),
    )
  end buildTrueFalseChoice

  private def getMultipleSelectPointsMap(item: Node): Map[String, Double] =
    /* and is optional */
    val parent = (item \ "resprocessing" \ "respcondition" \ "conditionvar" \ "and") ++
      (item \ "resprocessing" \ "respcondition" \ "conditionvar")

    val numCorrect    = (parent \ "varequal").size
    val correctPoints = 1d / numCorrect

    val numIncorrect    = (parent \ "not").size
    val incorrectPoints = -1d / numIncorrect

    parent.head.child.collect {
      case varequal: Elem if varequal.label == "varequal" => varequal.text           -> correctPoints
      case not: Elem if not.label == "not"                => (not \ "varequal").text -> incorrectPoints
    }.toMap
  end getMultipleSelectPointsMap

  // This just ignores OR because it's usually used to glom the incorrect choices into one condition:
  // "or(B,C,D) => score=0, feedback=Wrong"... which is safe to ignore and treat as zero
  def getMultipleChoicePointsMap(item: Node): Map[String, Double] =
    val scoreVarName = getScoreVarName(item)
    (item \ "resprocessing" \ "respcondition")
      .map(n =>
        val id     = (n \ "conditionvar" \ "varequal").text
        val score  = getScore(n, scoreVarName)
        val points = if score > 0 then 1d else 0d
        id -> points
      )
      .toMap
  end getMultipleChoicePointsMap

  private def buildMultipleChoices(item: Node, questionTypeId: AssetTypeId): Seq[ChoiceContent] =
    val choiceLabelMap  = getChoiceLabelMap(item)
    val choicePointsMap = questionTypeId match
      case AssetTypeId.MultipleChoiceQuestion => getMultipleChoicePointsMap(item)
      case AssetTypeId.MultipleSelectQuestion => getMultipleSelectPointsMap(item)
      case t                                  => throw UnsupportedQuestionTypeException(getQuestionId(item), t.entryName)

    val choiceIds = getChoiceIds(item)
    choiceIds.zipWithIndex.map { case (id, index) =>
      val label     = choiceLabelMap(id)
      val points    = choicePointsMap.getOrElse(id, 0d)
      val isCorrect = points > 0d
      val feedback  = getDistractorFeedback(item, id)
      buildMultipleChoice(label, index, isCorrect, points, feedback)
    }
  end buildMultipleChoices

  private def buildMultipleChoice(
    label: String,
    index: Int,
    isCorrect: Boolean,
    points: Double,
    feedback: Option[String]
  ): ChoiceContent =
    ChoiceContent(
      choiceContent = Some(HtmlPart(label)),
      index = index,
      correct = isCorrect,
      points = points,
      correctChoiceFeedback = feedback.when(isCorrect).map(HtmlPart(_)),
      incorrectChoiceFeedback = feedback.unless(isCorrect).map(HtmlPart(_)),
    )

  private def getPointValue(isCorrect: Boolean): Int = if isCorrect then 1 else 0

  private def getScoreVarName(item: Node): String = (item \ "resprocessing" \ "outcomes" \ "decvar" \ "@varname").text

  private def getScore(respcondition: Node, scoreVarName: String): Double =
    def isScoreNode(setvar: Node) =
      val varname = (setvar \ "@varname").text
      varname.isEmpty || varname == scoreVarName

    (respcondition \ "setvar").find(isScoreNode).map(_.text.toDouble).getOrElse(0d)

  // http://www.imsglobal.org/question/qtiv1p2/imsqti_litev1p2.html#1404575

  // Feedback looks like...

  // resprocessing
  //   respcondition*
  //     conditionvar
  //     scoring
  //     displayfeedback

  // Where the conditionvar is a rule that describes if the condition is true and if so
  // what score to give and what response to display. Sometimes a multiple choice has
  // one condition per choice, and thus scoring / feedback per distractor. Sometimes
  // it has one condition "A" for true, and then one condition "or(B,C,D)" for false,
  // and thus scoring/feedback at the question level. It is obviously impossible to
  // distinguish whether feedback is per question or distractor for a true/false question.
  // For historic reasons we'll just treat T/F as always question-level feedback. It is
  // not obvious what multiple select could look like; there'll be a composite for correct
  // like "and(A,B,not(C),not(D))" which somewhat precludes per-distractor positive
  // feedback, although per-distractor negative feedback is possible. it could have
  // one condition for scoring and then multiple for feedback, I suppose, using
  // respcondition/continue=Yes, but would need to see it in the wild.

  // Much of this code is lazy in using a NodeSeq in preferences to an Option[Node].

  private def getDistractorFeedback(item: Node, id: String): Option[String] =
    val respConditions = getFeedbackConditions(item)
    (respConditions.length > 2).option(
      getFeedback(item, respConditions.filter(cond => (cond \ "conditionvar" \ "varequal").text == id))
    )

  private def getQuestionFeedback(item: Node): Option[(String, String)] =
    val respConditions = getFeedbackConditions(item)
    (respConditions.length <= 2).option(
      respConditions.partition(cond => getScore(cond, getScoreVarName(item)) > 0).umap(getFeedback(item, _))
    )

  private def getFeedbackConditions(item: Node): NodeSeq =
    (item \ "resprocessing" \ "respcondition").filter(cond => (cond \ "displayfeedback").nonEmpty)

  private def getFeedback(item: Node, respCondition: NodeSeq): String =
    val feedbackId = (respCondition \ "displayfeedback" \ "@linkrefid").text
    val f          = (item \ "itemfeedback").filter(n => (n \ "@ident").text == feedbackId)
    htmlText((f \ "material" \ "mattext") ++ (f \ "flow_mat" \ "material" \ "mattext"))

  private def htmlText(nodes: NodeSeq): String = nodes.foldLeft("") { case (acc, node) =>
    acc + htmlText(node)
  }

  private def htmlText(node: Node): String =
    val isHtml = (node \ "@texttype").text == "text/html"
    if isHtml then destyle(node.text) else StringEscapeUtils.escapeHtml4(node.text)

  private def destyle(html: String): String =
    RemoveTrailingBrRE.replaceFirstIn(RemoveLeadingStyleRE.replaceFirstIn(html, "$1$2"), "$1")
end Qti1QuestionImporter

object Qti1QuestionImporter:
  // Two random competency-like fields from COMPTIAA
  final val CompetencyMetadataFields = List("learning_objective", "accrediting_standard") // "question_topic"

  // Remove only the outer style on the HTML, presuming there may be valid inner styles
  // like emphasis or code style.
  final val RemoveLeadingStyleRE = """^\s*(<[^>]*)\sstyle="[^"]*"([^>]*>)""".r
  // Remove all the trailing newlines, such as <br>$ or <br />$ or <br clear="all" /></span>$
  final val RemoveTrailingBrRE   = """(?:<br[^>]*>\s*)+(</\w+>)?\s*$""".r
