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

package loi.asset.survey

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.authoring.AssetType
import loi.authoring.asset.AssetExtractor
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

final case class Survey1(
  title: String,
  keywords: String = "",
  disabled: Boolean = false,
  inline: Boolean = false,
  archived: Boolean = false,
  programmatic: Boolean = false, // A programmatic survey does not launch automatically
)

object Survey1:
  implicit val assetTypeForSurvey1: AssetType[Survey1] = new AssetType[Survey1](AssetTypeId.Survey1):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.Questions -> Set(
        AssetTypeId.EssayQuestion,
        AssetTypeId.LikertScaleQuestion1,
        AssetTypeId.MultipleChoiceQuestion,
        AssetTypeId.RatingScaleQuestion1,
        AssetTypeId.SurveyChoiceQuestion1,
        AssetTypeId.SurveyEssayQuestion1,
      )
    )

    override def validate(data: Survey1): ValidatedNel[String, Unit] =
      Validate.size("title", 1, 255)(data.title) *>
        Validate.size("keywords", max = 255)(data.keywords)

    override def index(
      data: Survey1
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument =
      AssetDataDocument(title = data.title.option)

    override def htmls(data: Survey1)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      Nil

  object Asset extends AssetExtractor[Survey1]
end Survey1
