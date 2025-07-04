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
import loi.asset.contentpart.{BlockPart, HtmlPart}

import java.util.UUID

/** Common properties of each question content type. Question content is an object on each asset question.
  */
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
trait QuestionContent:

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TITLE)
  def questionTitle: Option[String]

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TEXT)
  def questionText
    : Option[String] // dcm frontend has asset.title as default, but some imports from exports have this blank

  @JsonProperty(AssessmentConfigurationTypeConstants.ASSESSMENT_QUESTION_ALLOW_DISTRACTOR_RANDOMIZATION)
  def allowDistractorRandomization: Option[Boolean]

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_COMPLEX_TEXT)
  def questionComplexText: HtmlPart

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_CONTENT_BLOCK_TEXT)
  def questionContentBlockText: BlockPart

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_CORRECT_ANSWER_FEEDBACK)
  def richCorrectAnswerFeedback: Option[HtmlPart]

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_INCORRECT_ANSWER_FEEDBACK)
  def richIncorrectAnswerFeedback: Option[HtmlPart]

  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_POINTS_POSSIBLE)
  def pointsPossible: String

  def edgeIds: Set[UUID] =
    Option(questionComplexText).map(_.edgeIds).getOrElse(Set.empty) ++
      Option(questionContentBlockText).map(_.edgeIds).getOrElse(Set.empty) ++
      richCorrectAnswerFeedback.map(_.edgeIds).getOrElse(Set.empty) ++
      richIncorrectAnswerFeedback.map(_.edgeIds).getOrElse(Set.empty)

  @JsonIgnore
  def getPublishQuestionText(): Option[String]
end QuestionContent
