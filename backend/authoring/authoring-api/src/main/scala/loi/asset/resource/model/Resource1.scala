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

package loi.asset.resource.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
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
import loi.authoring.validate.Validate
import loi.cp.ltitool.AssetLtiToolConfiguration
import scaloi.syntax.option.*

import java.util.UUID
import javax.validation.constraints.{Min, Size}

final case class Resource1(
  @Size(min = 1, max = 255)
  title: String,
  iconAlt: String = "",
  @Min(value = 0L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  duration: Option[Long] = None,
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-book",
  resourceType: ResourceType = ResourceType.ReadingInstructions,
  embedCode: Option[String] = None,
  lti: Option[AssetLtiToolConfiguration] = None,
  instructions: BlockPart = BlockPart(Seq(HtmlPart())),
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
)

object Resource1:

  implicit val assetTypeForResource1: AssetType[Resource1] = new AssetType[Resource1](AssetTypeId.Resource1):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.InSystemResource -> AssetTypeId.FileTypes,
      Group.Resources        -> AssetTypeId.FileTypes,
      Group.Teaches          -> AssetTypeId.CompetencyTypes,
      Group.Hyperlinks       -> AssetTypeId.HyperlinkTypes,
      Group.Survey           -> Set(AssetTypeId.Survey1),
    )

    override def validate(data: Resource1): ValidatedNel[String, Unit] =
      Validate.size("title", 1, 255)(data.title) *>
        Validate.min("duration", 0)(data.duration) *>
        Validate.size("keywords", max = 255)(data.keywords)

    override def edgeIds(data: Resource1): Set[UUID] =
      Option(data.instructions).map(_.edgeIds).getOrElse(Set.empty)

    override def render(data: Resource1, targets: Map[UUID, Asset[?]]): Resource1 =
      data.copy(instructions = Option(data.instructions).map(_.render(targets)).orNull)

    override def index(
      data: Resource1
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
      title = data.title.option,
      keywords = data.keywords.option,
      license = data.license,
      author = data.author,
      attribution = data.attribution,
      instructions = stringifyOpt(data.instructions)
    )

    override def htmls(
      data: Resource1
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      data.instructions.htmls

  object Asset extends AssetExtractor[Resource1]
end Resource1
