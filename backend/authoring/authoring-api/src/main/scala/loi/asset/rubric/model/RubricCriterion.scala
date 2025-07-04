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

import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.AssetExtractor
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.{AssetDataDocument, Strings}
import loi.authoring.syntax.index.*
import scaloi.syntax.option.*

import javax.validation.constraints.{Min, Size}

final case class RubricCriterion(
  @Size(min = 1, max = 255)
  title: String,
  description: String = "",
  archived: Boolean = false,
  levels: Seq[RubricCriterionLevel] = Seq.empty[RubricCriterionLevel],
  license: Option[License] = None,
  author: Option[String],
  attribution: Option[String]
)

object RubricCriterion:

  implicit val assetTypeForRubricCriterion: AssetType[RubricCriterion] =
    new AssetType[RubricCriterion](AssetTypeId.RubricCriterion):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Assesses -> AssetTypeId.CompetencyTypes
      )

      override def index(
        data: RubricCriterion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = data.title.option,
        description = data.description.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        content = stringifyOpt(data.levels)
      )

      override def htmls(
        data: RubricCriterion
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] = Nil

  object Asset extends AssetExtractor[RubricCriterion]
end RubricCriterion

case class RubricCriterionLevel(
  @Min(value = 0L)
  points: Int = 0,
  name: String = "",
  description: String = ""
)

object RubricCriterionLevel:

  implicit val rubricCriterionLevelStrings: Strings[RubricCriterionLevel] =
    Strings.plaintext(a => a.name :: a.description :: Nil)
