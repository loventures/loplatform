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
import loi.authoring.AssetType
import loi.authoring.asset.AssetExtractor
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.index.AssetDataDocument
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

final case class RatingScaleQuestion1(
  title: String,
  max: Int,
  lowRatingText: String,
  highRatingText: String,
  keywords: String = "",
  archived: Boolean = false,
  contentStatus: Option[String] = None,
)

object RatingScaleQuestion1:

  implicit val assetTypeForRatingScaleQuestion1: AssetType[RatingScaleQuestion1] =
    new AssetType[RatingScaleQuestion1](AssetTypeId.RatingScaleQuestion1):
      override def validate(data: RatingScaleQuestion1): ValidatedNel[String, Unit] =
        Validate.size("title", 1, 255)(data.title) *>
          Validate.min("max", 2)(data.max) *>
          Validate.notEmpty("lowRatingText")(data.lowRatingText) *>
          Validate.notEmpty("highRatingText")(data.highRatingText)

      override def index(
        data: RatingScaleQuestion1
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        keywords = data.keywords.option,
        content = data.title.option
      )

      override def htmls(
        data: RatingScaleQuestion1
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] = Nil

  object Asset extends AssetExtractor[RatingScaleQuestion1]
end RatingScaleQuestion1
