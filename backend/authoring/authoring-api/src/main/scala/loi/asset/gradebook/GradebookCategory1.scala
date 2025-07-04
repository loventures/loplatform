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

package loi.asset.gradebook

import cats.data.ValidatedNel
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.authoring.AssetType
import loi.authoring.asset.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.index.AssetDataDocument
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
final case class GradebookCategory1(
  @Size(min = 1, max = 255)
  title: String,
  weight: BigDecimal,
  archived: Boolean = false,
)

object GradebookCategory1:

  implicit val assetTypeForGradebookCategory1: AssetType[GradebookCategory1] =
    new AssetType[GradebookCategory1](AssetTypeId.GradebookCategory):

      override def validate(data: GradebookCategory1): ValidatedNel[String, Unit] =
        Validate.size("title", 1, 255)(data.title)

      override def index(
        data: GradebookCategory1
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = data.title.option,
      )

      override def htmls(
        data: GradebookCategory1
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] = Nil

  object Asset extends AssetExtractor[GradebookCategory1]
end GradebookCategory1
