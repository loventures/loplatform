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

package loi.asset.question

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonProperty}
import com.learningobjects.cpxp.service.component.misc.{
  AssessmentConfigurationTypeConstants,
  AssessmentQuestionTypeConstants
}
import com.learningobjects.cpxp.service.data.DataTypes
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.authoring.asset.Asset

import java.util.UUID
import scala.annotation.nowarn

@JsonIgnoreProperties(
  Array(
    AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_CORRECT_ANSWER_FEEDBACK,
    AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_INCORRECT_ANSWER_FEEDBACK,
    AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_ALLOWS_PARTIAL_CREDIT,
    AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_GRADING_TYPE,
    "questionType",
    "name"
  )
)
case class ChoiceQuestionContent(
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TITLE)
  questionTitle: Option[String] = None,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TEXT)
  questionText: Option[String] = None,
  @JsonProperty(AssessmentConfigurationTypeConstants.ASSESSMENT_QUESTION_ALLOW_DISTRACTOR_RANDOMIZATION)
  allowDistractorRandomization: Option[Boolean] = Some(true),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_COMPLEX_TEXT)
  questionComplexText: HtmlPart,           // in frontend, HtmlPart(html = asset.title, renderedHtml = asset.title)
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_CONTENT_BLOCK_TEXT)
  questionContentBlockText: BlockPart =
    BlockPart(),                           // in frontend, BlockPart(renderedHtml = asset.title, Seq(HtmlPart(html = asset.title, renderedHtml = asset.title)))
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_CORRECT_ANSWER_FEEDBACK)
  richCorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_INCORRECT_ANSWER_FEEDBACK)
  richIncorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_POINTS_POSSIBLE)
  pointsPossible: String = "1",
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_MULTI_CHOICE_QUESTION_CHOICE_LISTING_TYPE)
  choiceListingType: Option[String] = None,
  @JsonProperty(AssessmentQuestionTypeConstants.ITEM_TYPE_ASSESSMENT_QUESTION_CHOICE)
  choices: Seq[ChoiceContent] = Seq.empty, // in frontend, slightly different for MC, MS, T/F
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_MULTI_CHOICE_QUESTION_CHOICE_INLINE_FEEDBACK_FLAG)
  choiceInlineFeedback: Boolean = false
) extends QuestionContent:

  override def edgeIds: Set[UUID] =
    val edgeIds       = super.edgeIds
    val choiceEdgeIds = Option(choices)
      .map(choices => choices.flatMap(_.edgeIds))
      .getOrElse(Seq.empty)

    edgeIds ++ choiceEdgeIds

  def render(targets: Map[UUID, Asset[?]]): ChoiceQuestionContent =
    copy(
      questionComplexText = Option(questionComplexText).map(_.render(targets)).orNull,
      questionContentBlockText = Option(questionContentBlockText).map(_.render(targets)).orNull,
      richCorrectAnswerFeedback = richCorrectAnswerFeedback.map(_.render(targets)),
      richIncorrectAnswerFeedback = richIncorrectAnswerFeedback.map(_.render(targets)),
      choices = Option(choices)
        .map(choices => choices.map(_.render(targets)))
        .getOrElse(List.empty)
    )

  def receiveTitle(title: String): ChoiceQuestionContent =
    copy(
      questionComplexText = HtmlPart(title),
      questionText = Option(title)
    )

  @JsonIgnore
  override def getPublishQuestionText(): Option[String] = Option(questionComplexText).flatMap(_.renderedHtml)
end ChoiceQuestionContent

/** @tparam C
  *   the type of question choice. Used by MultipleSelectQuestion, MultipleChoiceQuestion, TrueFalseQuestion. Extended
  *   by OrderingChoice in OrderingQuestion.
  */
trait ChoiceContentTrait[C <: ChoiceContentTrait[C]]:
  self: C =>

  @Deprecated @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_TEXT)
  def choiceText: Option[String]

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_COMPLEX_TEXT)
  def choiceContent: Option[HtmlPart]

  @JsonProperty(DataTypes.DATA_TYPE_INDEX)
  def index: Long

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_CORRECT)
  def correct: Boolean

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_POINT_VALUE)
  def points: Double

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_CORRECT_FEEDBACK)
  def correctChoiceFeedback: Option[HtmlPart] = Some(HtmlPart())

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_INCORRECT_FEEDBACK)
  def incorrectChoiceFeedback: Option[HtmlPart] = Some(HtmlPart())

  def edgeIds: Set[UUID] =
    choiceContent.map(_.edgeIds).getOrElse(Set.empty) ++
      correctChoiceFeedback.map(_.edgeIds).getOrElse(Set.empty) ++
      incorrectChoiceFeedback.map(_.edgeIds).getOrElse(Set.empty)

  // noinspection ScalaDeprecation
  def description: HtmlPart =
    choiceContent.getOrElse(
      HtmlPart.apply(html = choiceText.getOrElse(""): @nowarn, renderedHtml = None)
    )
end ChoiceContentTrait

case class ChoiceContent(
  @Deprecated @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_TEXT)
  choiceText: Option[String] = None,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_COMPLEX_TEXT)
  choiceContent: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(DataTypes.DATA_TYPE_INDEX)
  index: Long = 0,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_CORRECT)
  correct: Boolean,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_POINT_VALUE)
  points: Double = 0.0,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_CORRECT_FEEDBACK)
  override val correctChoiceFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_INCORRECT_FEEDBACK)
  override val incorrectChoiceFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some("")))
) extends ChoiceContentTrait[ChoiceContent]:

  def render(targets: Map[UUID, Asset[?]]): ChoiceContent =
    copy(
      choiceContent = choiceContent.map(_.render(targets)),
      correctChoiceFeedback = correctChoiceFeedback.map(_.render(targets)),
      incorrectChoiceFeedback = incorrectChoiceFeedback.map(_.render(targets))
    )
end ChoiceContent
