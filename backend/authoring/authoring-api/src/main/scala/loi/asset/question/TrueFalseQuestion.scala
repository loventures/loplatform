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
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.asset.{Asset, AssetExtractor}
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.syntax.index.*
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import java.util.UUID
import javax.validation.constraints.{NotEmpty, Size}

final case class TrueFalseQuestion(
  questionContent: ChoiceQuestionContent,
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

object TrueFalseQuestion:

  implicit val assetTypeForTrueFalseQuestion: AssetType[TrueFalseQuestion] =
    new AssetType[TrueFalseQuestion](AssetTypeId.TrueFalseQuestion):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Assesses             -> AssetTypeId.CompetencyTypes,
        Group.RemediationResources -> AssetTypeId.RemediationTypes,
        Group.Resources            -> AssetTypeId.FileTypes,
      )

      override def validate(data: TrueFalseQuestion): ValidatedNel[String, Unit] =
        Validate.notEmpty("title")(data.title) *> Validate.size("keywords", max = 255)(data.keywords)

      override def updateValidate(
        data: TrueFalseQuestion,
        groupSizes: => Map[Group, Int],
      ): ValidatedNel[String, Unit] =
        validate(data) *> Validated.condNel(
          Option(data.questionContent.choices).exists(_.count(_.correct) == 1),
          (),
          "question must have exactly one correct choice"
        )

      override def computeTitle(data: TrueFalseQuestion): Option[String] = Some(
        Option(data.questionContent.questionComplexText).map(_.plainText).getOrElse("")
      )

      override def receiveTitle(data: TrueFalseQuestion, title: String): TrueFalseQuestion =
        data.copy(questionContent = data.questionContent.receiveTitle(title))

      override def edgeIds(data: TrueFalseQuestion): Set[UUID] = data.questionContent.edgeIds

      override def render(data: TrueFalseQuestion, targets: Map[UUID, Asset[?]]): TrueFalseQuestion =
        data.copy(questionContent = data.questionContent.render(targets))

      override def index(
        data: TrueFalseQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = None, // == content.questionText: a.title.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        content = stringifyOpt(data.questionContent)
      )

      override def htmls(
        data: TrueFalseQuestion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.questionContent.htmls

  object Asset extends AssetExtractor[TrueFalseQuestion]
end TrueFalseQuestion
