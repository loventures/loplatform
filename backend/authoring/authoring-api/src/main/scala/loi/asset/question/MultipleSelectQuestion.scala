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
import com.learningobjects.cpxp.service.mime.MimeWebService
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

final case class MultipleSelectQuestion(
  questionContent: ChoiceQuestionContent,
  allowPartialCredit: Boolean = false,
  scoringOption: Option[QuestionScoringOption] = None,
  @NotEmpty
  title: String,
  archived: Boolean = false,
  @Size(min = 0, max = 255)
  keywords: String = "",
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  contentStatus: Option[String] = None,
)

object MultipleSelectQuestion:

  implicit val assetTypeForMultipleSelectQuestion: AssetType[MultipleSelectQuestion] =
    new AssetType[MultipleSelectQuestion](AssetTypeId.MultipleSelectQuestion):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Assesses             -> AssetTypeId.CompetencyTypes,
        Group.RemediationResources -> AssetTypeId.RemediationTypes,
        Group.Resources            -> AssetTypeId.FileTypes,
      )

      override def validate(data: MultipleSelectQuestion): ValidatedNel[String, Unit] =
        Validate.notEmpty("title")(data.title) *> Validate.size("keywords", max = 255)(data.keywords)

      override def computeTitle(data: MultipleSelectQuestion): Option[String] = Some(
        Option(data.questionContent.questionComplexText).map(_.plainText).getOrElse("")
      )

      override def receiveTitle(data: MultipleSelectQuestion, title: String): MultipleSelectQuestion =
        data.copy(questionContent = data.questionContent.receiveTitle(title))

      override def edgeIds(data: MultipleSelectQuestion): Set[UUID] = data.questionContent.edgeIds

      override def render(data: MultipleSelectQuestion, targets: Map[UUID, Asset[?]]): MultipleSelectQuestion =
        data.copy(questionContent = data.questionContent.render(targets))

      override def index(
        data: MultipleSelectQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = None, // == content.questionText: a.title.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        content = stringifyOpt(data.questionContent)
      )

      override def htmls(
        data: MultipleSelectQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.questionContent.htmls

  object Asset extends AssetExtractor[MultipleSelectQuestion]
end MultipleSelectQuestion
