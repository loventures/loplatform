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
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.learningobjects.cpxp.service.component.misc.{
  AssessmentConfigurationTypeConstants,
  AssessmentQuestionTypeConstants
}
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.util.HtmlUtils
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

final case class EssayQuestion(
  @NotEmpty
  title: String,
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  questionContent: EssayContent,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  contentStatus: Option[String] = None,
)

object EssayQuestion:

  implicit val assetTypeForEssayQuestion: AssetType[EssayQuestion] =
    new AssetType[EssayQuestion](AssetTypeId.EssayQuestion):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Assesses             -> AssetTypeId.CompetencyTypes,
        Group.CblRubric            -> Set(AssetTypeId.Rubric),
        Group.RemediationResources -> AssetTypeId.RemediationTypes,
        Group.Resources            -> AssetTypeId.FileTypes,
      )

      override def validate(data: EssayQuestion): ValidatedNel[String, Unit] =
        Validate.notEmpty("title")(data.title) *> Validate.size("keywords", max = 255)(data.keywords)

      override def computeTitle(data: EssayQuestion): Option[String] =
        Option(data.questionContent.questionContentBlockText)
          .map(blockPart =>
            blockPart.parts
              .flatMap({
                case htmlPart: HtmlPart => Some(HtmlUtils.toPlaintext(htmlPart.html))
                case _                  => None
              })
              .mkString(" ")
          )

      override def receiveTitle(data: EssayQuestion, title: String): EssayQuestion =
        data.copy(questionContent = data.questionContent.receiveTitle(title))

      override def edgeIds(data: EssayQuestion): Set[UUID] = data.questionContent.edgeIds

      override def render(data: EssayQuestion, targets: Map[UUID, Asset[?]]): EssayQuestion =
        data.copy(questionContent = data.questionContent.render(targets))

      override def index(
        data: EssayQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = None, // == content.questionText: a.title.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        content = stringifyOpt(data.questionContent)
      )

      override def htmls(
        data: EssayQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.questionContent.htmls

  object Asset extends AssetExtractor[EssayQuestion]
end EssayQuestion

case class EssayContent(
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TITLE)
  questionTitle: Option[String] = None,                              // UNUSED
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_TEXT)
  questionText: Option[String] = None,                               // UNUSED
  @JsonProperty(AssessmentConfigurationTypeConstants.ASSESSMENT_QUESTION_ALLOW_DISTRACTOR_RANDOMIZATION)
  allowDistractorRandomization: Option[Boolean] = Some(true),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_COMPLEX_TEXT)
  questionComplexText: HtmlPart = HtmlPart(renderedHtml = Some("")), // UNUSED
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_CONTENT_BLOCK_TEXT)
  questionContentBlockText: BlockPart =
    BlockPart(),                                                     //  in frontend, 'html' and 'renderedHtml for BlockPart and first HtmlPart default to 'asset.title'
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_CORRECT_ANSWER_FEEDBACK)
  richCorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_RICH_INCORRECT_ANSWER_FEEDBACK)
  richIncorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_QUESTION_POINTS_POSSIBLE)
  pointsPossible: String = "1"
) extends QuestionContent:

  def render(targets: Map[UUID, Asset[?]]): EssayContent =
    copy(
      questionComplexText = Option(questionComplexText).map(_.render(targets)).orNull,
      questionContentBlockText = Option(questionContentBlockText).map(_.render(targets)).orNull,
      richCorrectAnswerFeedback = richCorrectAnswerFeedback.map(_.render(targets)),
      richIncorrectAnswerFeedback = richIncorrectAnswerFeedback.map(_.render(targets))
    )

  def receiveTitle(title: String): EssayContent =
    copy(
      questionContentBlockText = BlockPart(parts = Seq(HtmlPart(title))),
      questionText = Option(title)
    )

  @JsonIgnore
  override def getPublishQuestionText(): Option[String] = Option(questionContentBlockText).flatMap(_.renderedHtml)
end EssayContent
