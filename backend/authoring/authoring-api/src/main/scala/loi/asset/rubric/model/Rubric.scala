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

package loi.asset.rubric.model

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
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import javax.validation.constraints.Size

final case class Rubric(
  @Size(min = 1, max = 255)
  title: String,
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  license: Option[License] = None,
  author: Option[String],
  attribution: Option[String]
)

object Rubric:

  implicit val assetTypeForRubric: AssetType[Rubric] = new AssetType[Rubric](AssetTypeId.Rubric):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.Criteria -> Set(AssetTypeId.RubricCriterion)
    )

    override def validate(data: Rubric): ValidatedNel[String, Unit] =
      Validate.size("title", 1, 255)(data.title) *> Validate.size("keywords", max = 255)(data.keywords)

    override def index(
      data: Rubric
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
      title = data.title.option,
      license = data.license,
      author = data.author,
      attribution = data.attribution,
      instructions = None
    )

    override def htmls(data: Rubric)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      Nil

  object Asset extends AssetExtractor[Rubric]
end Rubric
