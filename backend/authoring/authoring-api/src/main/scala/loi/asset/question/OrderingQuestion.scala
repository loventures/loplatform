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

import cats.data.{Validated, ValidatedNel}
import cats.syntax.apply.*
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonProperty}
import com.learningobjects.cpxp.service.component.misc.{
  AssessmentConfigurationTypeConstants,
  AssessmentQuestionTypeConstants
}
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.syntax.index.*
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import java.util.UUID
import javax.validation.constraints.{NotEmpty, Size}

final case class OrderingQuestion(
  @NotEmpty
  title: String,
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  questionContent: OrderingContent,
  allowPartialCredit: Boolean = false,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  contentStatus: Option[String] = None,
)

object OrderingQuestion:

  implicit val assetTypeForOrderingQuestion: AssetType[OrderingQuestion] =
    new AssetType[OrderingQuestion](AssetTypeId.OrderingQuestion):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Assesses             -> AssetTypeId.CompetencyTypes,
        Group.RemediationResources -> AssetTypeId.RemediationTypes,
        Group.Resources            -> AssetTypeId.FileTypes,
      )

      override def validate(data: OrderingQuestion): ValidatedNel[String, Unit] =
        Validate.notEmpty("title")(data.title) *> Validate.size("keywords", max = 255)(data.keywords)

      override def updateValidate(
        data: OrderingQuestion,
        groupSizes: => Map[Group, Int]
      ): ValidatedNel[String, Unit] =
        validate(data) *> Validated.condNel(
          Option(data.questionContent.choices).exists(_.nonEmpty),
          (),
          "ordering questions must have at least one choice"
        )

      override def computeTitle(data: OrderingQuestion): Option[String] = Some(
        Option(data.questionContent.questionComplexText).map(_.plainText).getOrElse("")
      )

      override def receiveTitle(data: OrderingQuestion, title: String): OrderingQuestion =
        data.copy(questionContent = data.questionContent.receiveTitle(title))

      override def edgeIds(data: OrderingQuestion): Set[UUID] = data.questionContent.edgeIds

      override def render(data: OrderingQuestion, targets: Map[UUID, Asset[?]]): OrderingQuestion =
        data.copy(questionContent = data.questionContent.render(targets))

      override def index(
        data: OrderingQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = None, // == content.questionText: a.title.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        content = stringifyOpt(data.questionContent)
      )

      override def htmls(
        data: OrderingQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.questionContent.htmls

  object Asset extends AssetExtractor[OrderingQuestion]
end OrderingQuestion

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
case class OrderingContent(
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TITLE)
  questionTitle: Option[String] = None,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TEXT)
  questionText: Option[String] = None,
  @JsonProperty(AssessmentConfigurationTypeConstants.ASSESSMENT_QUESTION_ALLOW_DISTRACTOR_RANDOMIZATION)
  allowDistractorRandomization: Option[Boolean] = Some(true),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_COMPLEX_TEXT)
  questionComplexText: HtmlPart = HtmlPart(), // frontend defaults to "HtmlPart(asset.title)"
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_CONTENT_BLOCK_TEXT)
  questionContentBlockText: BlockPart =
    BlockPart(),                              // frontend defaults to "BlockPart(HtmlPart(html = asset.title, renderedHtml = asset.title), renderedHtml = asset.title, html = asset.title)"
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_CORRECT_ANSWER_FEEDBACK)
  richCorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_INCORRECT_ANSWER_FEEDBACK)
  richIncorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_POINTS_POSSIBLE)
  pointsPossible: String = "1",
  @JsonProperty(AssessmentQuestionTypeConstants.ITEM_TYPE_ASSESSMENT_ORDERING_QUESTION_CHOICE)
  choices: Seq[OrderingChoice] = Seq.empty
) extends QuestionContent:

  override def edgeIds: Set[UUID] =
    val edgeIds            = super.edgeIds
    val orderChoiceEdgeIds = Option(choices)
      .map(choices => choices.flatMap(_.edgeIds))
      .getOrElse(Seq.empty)

    edgeIds ++ orderChoiceEdgeIds

  def render(targets: Map[UUID, Asset[?]]): OrderingContent =
    copy(
      questionComplexText = Option(questionComplexText).map(_.render(targets)).orNull,
      questionContentBlockText = Option(questionContentBlockText).map(_.render(targets)).orNull,
      richCorrectAnswerFeedback = richCorrectAnswerFeedback.map(_.render(targets)),
      richIncorrectAnswerFeedback = richIncorrectAnswerFeedback.map(_.render(targets)),
      choices = Option(choices)
        .map(choices => choices.map(_.render(targets)))
        .getOrElse(List.empty)
    )

  def receiveTitle(title: String): OrderingContent =
    copy(
      questionComplexText = HtmlPart(title),
      questionText = Option(title)
    )

  @JsonIgnore
  override def getPublishQuestionText(): Option[String] = Option(questionComplexText).flatMap(_.renderedHtml)
end OrderingContent

case class OrderingChoice(
  @Deprecated @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_TEXT)
  choiceText: Option[String] = None,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_COMPLEX_TEXT)
  choiceContent: Option[HtmlPart],
  @JsonProperty(DataTypes.DATA_TYPE_INDEX)
  index: Long = 0,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_CORRECT)
  correct: Boolean = false,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_POINT_VALUE)
  points: Double = 0.0,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_CORRECT_FEEDBACK)
  override val correctChoiceFeedback: Option[HtmlPart] = Some(HtmlPart()),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_INCORRECT_FEEDBACK)
  override val incorrectChoiceFeedback: Option[HtmlPart] = Some(HtmlPart()),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_ORDERING_QUESTION_CHOICE_ANSWER_INDEX)
  answerIndex: Long,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_ORDERING_QUESTION_CHOICE_RENDERING_INDEX)
  renderingIndex: Long = 0,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_CHOICE_IDENTIFIER)
  choiceIdentifier: Option[String] = None
) extends ChoiceContentTrait[OrderingChoice]:
  def render(targets: Map[UUID, Asset[?]]): OrderingChoice =
    copy(
      choiceContent = choiceContent.map(_.render(targets)),
      correctChoiceFeedback = correctChoiceFeedback.map(_.render(targets)),
      incorrectChoiceFeedback = incorrectChoiceFeedback.map(_.render(targets))
    )
end OrderingChoice
