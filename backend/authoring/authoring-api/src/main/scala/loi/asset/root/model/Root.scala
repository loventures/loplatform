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

package loi.asset.root.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.AssetExtractor
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import javax.validation.constraints.Size

final case class Root(
  @Size(min = 1, max = 255)
  title: String,
  @Size(min = 0, max = 255)
  subtitle: String = "",
  description: Option[String] = None,
  archived: Boolean = false,
  @Size(min = 0, max = 255)
  keywords: String = "",
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  projectStatus: Option[String] = None,
)

object Root:

  implicit val assetTypeForRoot: AssetType[Root] = new AssetType[Root](AssetTypeId.Root):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.CompetencySets -> Set(AssetTypeId.CompetencySet),
      Group.Courses        -> Set(AssetTypeId.Course),
    )

    override def validate(data: Root): ValidatedNel[String, Unit] =
      Validate.size("title", 1, 255)(data.title) *>
        Validate.size("subtitle", max = 255)(data.subtitle) *>
        Validate.size("keywords", max = 255)(data.keywords)

    override def index(
      data: Root
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
      title = data.title.option,
      subtitle = data.subtitle.option,
      description = data.description,
      keywords = data.keywords.option,
      license = data.license,
      author = data.author,
      attribution = data.attribution,
      instructions = None
    )

    override def htmls(data: Root)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      Nil

  object Asset extends AssetExtractor[Root]
end Root
