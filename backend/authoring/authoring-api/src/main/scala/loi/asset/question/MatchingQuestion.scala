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

import cats.data.ValidatedNel
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

final case class MatchingQuestion(
  @NotEmpty
  title: String,
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  questionContent: MatchingContent,
  allowPartialCredit: Boolean = false,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  contentStatus: Option[String] = None,
)

object MatchingQuestion:

  implicit val assetTypeForMatchingQuestion: AssetType[MatchingQuestion] =
    new AssetType[MatchingQuestion](AssetTypeId.MatchingQuestion):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Assesses             -> AssetTypeId.CompetencyTypes,
        Group.RemediationResources -> AssetTypeId.RemediationTypes,
        Group.Resources            -> AssetTypeId.FileTypes,
      )

      override def validate(data: MatchingQuestion): ValidatedNel[String, Unit] =
        Validate.notEmpty("title")(data.title) *> Validate.size("keywords", max = 255)(data.keywords)

      override def computeTitle(data: MatchingQuestion): Option[String] = Some(
        Option(data.questionContent.questionComplexText).map(_.plainText).getOrElse("")
      )

      override def receiveTitle(data: MatchingQuestion, title: String): MatchingQuestion =
        data.copy(questionContent = data.questionContent.receiveTitle(title))

      override def edgeIds(data: MatchingQuestion): Set[UUID] = data.questionContent.edgeIds

      override def render(data: MatchingQuestion, targets: Map[UUID, Asset[?]]): MatchingQuestion =
        data.copy(questionContent = data.questionContent.render(targets))

      override def index(
        data: MatchingQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = None, // == content.questionText: a.title.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        content = stringifyOpt(data.questionContent)
      )

      override def htmls(
        data: MatchingQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.questionContent.htmls

  object Asset extends AssetExtractor[MatchingQuestion]
end MatchingQuestion

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
case class MatchingContent(
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TITLE)
  questionTitle: Option[String] = None,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TEXT)
  questionText: Option[String] = None,
  @JsonProperty(AssessmentConfigurationTypeConstants.ASSESSMENT_QUESTION_ALLOW_DISTRACTOR_RANDOMIZATION)
  allowDistractorRandomization: Option[Boolean] = Some(true),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_COMPLEX_TEXT)
  questionComplexText: HtmlPart, // frontend has HtmlPart(html = asset.title, renderedHtml = "") as default
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_CONTENT_BLOCK_TEXT)
  questionContentBlockText: BlockPart =
    BlockPart(),                 // frontend has BlockPart(Seq(HtmlPart(html = asset.title, renderedHtml = asset.title)), renderedHtml = asset.title) as default
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_CORRECT_ANSWER_FEEDBACK)
  richCorrectAnswerFeedback: Option[HtmlPart] = Some(
    HtmlPart(renderedHtml = Some(""))
  ),                             // frontend has Some(HtmlPart("", "")) as default
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_INCORRECT_ANSWER_FEEDBACK)
  richIncorrectAnswerFeedback: Option[HtmlPart] = Some(
    HtmlPart(renderedHtml = Some(""))
  ),                             // frontend has Some(HtmlPart("", "")) as default
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_POINTS_POSSIBLE)
  pointsPossible: String = "1",
  @JsonProperty(
    AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_MATCHING_QUESTION_CHOICE_MULTIPLE_DEFINITIONS_PER_TERM_FLAG
  )
  multipleDefinitionsPerTermAllowed: Boolean = false,
  @JsonProperty(AssessmentQuestionTypeConstants.ITEM_TYPE_ASSESSMENT_QUESTION_TERM)
  terms: Seq[TermContent] = Seq.empty,
  @JsonProperty(AssessmentQuestionTypeConstants.ITEM_TYPE_ASSESSMENT_QUESTION_DEFINITION)
  definitionContent: Seq[DefinitionContent] = Seq.empty
) extends QuestionContent:

  def render(targets: Map[UUID, Asset[?]]): MatchingContent =
    copy(
      questionComplexText = Option(questionComplexText).map(_.render(targets)).orNull,
      questionContentBlockText = Option(questionContentBlockText).map(_.render(targets)).orNull,
      richCorrectAnswerFeedback = richCorrectAnswerFeedback.map(_.render(targets)),
      richIncorrectAnswerFeedback = richIncorrectAnswerFeedback.map(_.render(targets))
    )

  def receiveTitle(title: String): MatchingContent =
    copy(
      questionComplexText = HtmlPart(title),
      questionText = Option(title)
    )

  @JsonIgnore
  override def getPublishQuestionText(): Option[String] = Option(questionComplexText).flatMap(_.renderedHtml)
end MatchingContent

case class TermContent(
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_TERM_IDENTIFIER)
  termIdentifier: Option[String] =
    None,                                // to quote the frontend: "is this the id? it comes from qti imports. not particularly important"
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_TERM_TEXT)
  termText: String = "",                 // should be required, but old data blows up otherwise. See (need for) InvalidQuestionService.
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_TERM_POINT_VALUE)
  pointValue: Option[String] = None,     // unused
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_TERM_FEEDBACK_INLINE)
  feedbackInline: Option[String] = None, // unused
  @JsonProperty(DataTypes.DATA_TYPE_INDEX)
  index: Option[Long] = None,            // unused
  @JsonProperty("correctDefinitionIndex")
  correctIndex: Long
)

case class DefinitionContent(
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_QUESTION_DEFINITION_TEXT)
  definitionText: String =
    "", // should be required, but old data blows up otherwise. See (need for) InvalidQuestionService.
  @JsonProperty(DataTypes.DATA_TYPE_INDEX)
  index: Long
)
