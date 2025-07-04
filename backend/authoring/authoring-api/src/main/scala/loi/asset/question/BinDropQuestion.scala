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
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude, JsonProperty}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
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

import java.util.UUID
import javax.validation.constraints.{NotEmpty, Size}

final case class BinDropQuestion(
  @NotEmpty
  title: String,
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  questionContent: BinDropContent,
  allowPartialCredit: Boolean = false,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  contentStatus: Option[String] = None,
)

object BinDropQuestion:

  implicit val assetTypeForBinDropQuestion: AssetType[BinDropQuestion] =
    new AssetType[BinDropQuestion](AssetTypeId.BinDropQuestion):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Assesses             -> AssetTypeId.CompetencyTypes,
        Group.RemediationResources -> AssetTypeId.RemediationTypes,
        Group.Resources            -> AssetTypeId.FileTypes,
      )

      override def validate(data: BinDropQuestion): ValidatedNel[String, Unit] =
        Validate.notEmpty("title")(data.title) *> Validate.size("keywords", max = 255)(data.keywords)

      override def computeTitle(data: BinDropQuestion): Option[String] = Some(
        Option(data.questionContent.questionComplexText).map(_.plainText).getOrElse("")
      )

      override def receiveTitle(data: BinDropQuestion, title: String): BinDropQuestion =
        data.copy(questionContent = data.questionContent.receiveTitle(title))

      override def edgeIds(data: BinDropQuestion): Set[UUID] = data.questionContent.edgeIds

      override def render(data: BinDropQuestion, targets: Map[UUID, Asset[?]]): BinDropQuestion =
        data.copy(questionContent = data.questionContent.render(targets))

      override def index(
        data: BinDropQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = None, // == content.questionText: a.title.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        content = stringifyOpt(data.questionContent)
      )

      override def htmls(
        data: BinDropQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.questionContent.htmls

  object Asset extends AssetExtractor[BinDropQuestion]
end BinDropQuestion

case class BinDropContent(
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TITLE)
  questionTitle: Option[String] = None,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TEXT)
  questionText: Option[String] = None,
  @JsonProperty(AssessmentConfigurationTypeConstants.ASSESSMENT_QUESTION_ALLOW_DISTRACTOR_RANDOMIZATION)
  allowDistractorRandomization: Option[Boolean] = Some(true),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_COMPLEX_TEXT)
  questionComplexText: HtmlPart,                     // frontend has html as "asset.title" by default
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_CONTENT_BLOCK_TEXT)
  questionContentBlockText: BlockPart = BlockPart(), // frontend has html as "asset.title" by default
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_CORRECT_ANSWER_FEEDBACK)
  richCorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_INCORRECT_ANSWER_FEEDBACK)
  richIncorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_POINTS_POSSIBLE)
  pointsPossible: String = "1",
  @JsonProperty(AssessmentQuestionTypeConstants.ITEM_TYPE_BIN_DROP_BIN)
  bins: Seq[BinContent] = Seq.empty,
  @JsonProperty(AssessmentQuestionTypeConstants.ITEM_TYPE_BIN_DROP_OPTION)
  options: Seq[OptionContent] = Seq.empty
) extends QuestionContent:

  def render(targets: Map[UUID, Asset[?]]): BinDropContent =
    copy(
      questionComplexText = Option(questionComplexText).map(_.render(targets)).orNull,
      questionContentBlockText = Option(questionContentBlockText).map(_.render(targets)).orNull,
      richCorrectAnswerFeedback = richCorrectAnswerFeedback.map(_.render(targets)),
      richIncorrectAnswerFeedback = richIncorrectAnswerFeedback.map(_.render(targets))
    )

  def receiveTitle(title: String): BinDropContent       =
    copy(
      questionComplexText = HtmlPart(title),
      questionText = Option(title)
    )
  @JsonIgnore
  override def getPublishQuestionText(): Option[String] = Option(questionComplexText).flatMap(_.renderedHtml)
end BinDropContent

case class BinContent(
  @JsonProperty(DataTypes.DATA_TYPE_INDEX) index: Long,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_BIN_DROP_BIN_LABEL) label: String
)

case class OptionContent(
  @JsonProperty(DataTypes.DATA_TYPE_INDEX)
  index: Long,
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_BIN_DROP_OPTION_LABEL)
  label: String = "", // should be required, but old data blows up otherwise. See (need for) InvalidQuestionService.
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  @JsonInclude(Include.NON_NULL)
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_BIN_DROP_OPTION_BIN)
  binIndex: Option[Long]
)
