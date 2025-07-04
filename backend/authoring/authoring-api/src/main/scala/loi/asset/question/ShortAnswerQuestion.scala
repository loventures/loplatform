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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.service.component.misc.AssessmentQuestionTypeConstants
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
import scaloi.syntax.option.*

import java.util.UUID

final case class ShortAnswerQuestion(
  questionContent: ShortAnswerContent,
  license: Option[License] = None,
  author: String = "",
  attribution: String = "",
  contentStatus: Option[String] = None,
)

object ShortAnswerQuestion:

  implicit val assetTypeForShortAnswerQuestion: AssetType[ShortAnswerQuestion] =
    new AssetType[ShortAnswerQuestion](AssetTypeId.ShortAnswerQuestion):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Assesses             -> AssetTypeId.CompetencyTypes,
        Group.RemediationResources -> AssetTypeId.RemediationTypes,
        Group.Resources            -> AssetTypeId.FileTypes,
      )

      override def computeTitle(data: ShortAnswerQuestion): Option[String] = Some(
        Option(data.questionContent.questionComplexText).map(_.plainText).getOrElse("")
      )

      override def receiveTitle(data: ShortAnswerQuestion, title: String): ShortAnswerQuestion =
        data.copy(questionContent = data.questionContent.receiveTitle(title))

      override def edgeIds(data: ShortAnswerQuestion): Set[UUID] = data.questionContent.edgeIds

      override def render(data: ShortAnswerQuestion, targets: Map[UUID, Asset[?]]): ShortAnswerQuestion =
        data.copy(questionContent = data.questionContent.render(targets))

      override def index(
        data: ShortAnswerQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        license = data.license,
        author = data.author.option,
        attribution = data.attribution.option,
        content = stringifyOpt(data.questionContent)
      )

      override def htmls(
        data: ShortAnswerQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.questionContent.htmls

  object Asset extends AssetExtractor[ShortAnswerQuestion]
end ShortAnswerQuestion

case class ShortAnswerContent(
  questionTitle: Option[String] = None,
  questionText: Option[String] = None,
  allowDistractorRandomization: Option[Boolean] = Some(false),
  questionComplexText: HtmlPart = HtmlPart(renderedHtml = Some("")),
  questionContentBlockText: BlockPart = BlockPart(),
  richCorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  richIncorrectAnswerFeedback: Option[HtmlPart] = Some(HtmlPart(renderedHtml = Some(""))),
  pointsPossible: String = "1",
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_SHORT_ANSWER_QUESTION_ANSWER_WIDTH)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  width: Option[Long] = Some(1),
  @JsonProperty(AssessmentQuestionTypeConstants.DATA_TYPE_ASSESSMENT_SHORT_ANSWER_QUESTION_ANSWER)
  answer: String = ""
) extends QuestionContent:

  def render(targets: Map[UUID, Asset[?]]): ShortAnswerContent =
    copy(
      questionComplexText = Option(questionComplexText).map(_.render(targets)).orNull,
      questionContentBlockText = Option(questionContentBlockText).map(_.render(targets)).orNull,
      richCorrectAnswerFeedback = richCorrectAnswerFeedback.map(_.render(targets)),
      richIncorrectAnswerFeedback = richIncorrectAnswerFeedback.map(_.render(targets))
    )

  def receiveTitle(title: String): ShortAnswerContent =
    copy(
      questionComplexText = HtmlPart(title),
      questionText = Option(title)
    )

  override def getPublishQuestionText(): Option[String] = Option(questionComplexText).flatMap(_.renderedHtml)
end ShortAnswerContent
