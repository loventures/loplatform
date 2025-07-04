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

package loi.asset.assessment.model

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
import scaloi.syntax.option.*

import java.util.UUID
import javax.validation.constraints.{Min, Size}

final case class Checkpoint(
  @Size(min = 1, max = 255)
  title: String,
  iconAlt: String = "",
  @Size(min = 0, max = 255)
  subtitle: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-stack-check",
  name: Option[String] = None,
  instructions: BlockPart = BlockPart(Seq(HtmlPart())),
  license: Option[License] = None,
  @Size(min = 0, max = 255)
  author: Option[String] = None,
  @Size(min = 0, max = 255)
  attribution: Option[String] = None,
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
  @Min(value = 0L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  duration: Option[Long] = None,
)

object Checkpoint:

  implicit val assetTypeForCheckpoint: AssetType[Checkpoint] = new AssetType[Checkpoint](AssetTypeId.Checkpoint):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.Gates      -> AssetTypeId.GatedTypes,
      Group.Questions  -> AssetTypeId.QuestionTypes,
      Group.Resources  -> AssetTypeId.FileTypes,
      Group.Hyperlinks -> AssetTypeId.HyperlinkTypes,
      Group.Survey     -> Set(AssetTypeId.Survey1),
    )

    override def validate(data: Checkpoint): ValidatedNel[String, Unit] =
      Validate.size("title", min = 1, max = 255)(data.title) *>
        Validate.size("subtitle", max = 255)(data.subtitle) *>
        Validate.size("keywords", max = 255)(data.keywords) *>
        Validate.size("author", max = 255)(data.author) *>
        Validate.size("attribution", max = 255)(data.attribution)

    override def edgeIds(a: Checkpoint): Set[UUID] = Option(a.instructions).map(_.edgeIds).getOrElse(Set.empty)

    override def render(a: Checkpoint, targets: Map[UUID, Asset[?]]): Checkpoint =
      a.copy(instructions = Option(a.instructions).map(_.render(targets)).orNull)

    // data.name appears unused
    override def index(
      data: Checkpoint
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument =
      AssetDataDocument(
        title = data.title.option,
        subtitle = data.subtitle.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        instructions = stringifyOpt(data.instructions)
      )

    override def htmls(
      data: Checkpoint
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      data.instructions.htmls

  object Asset extends AssetExtractor[Checkpoint]
end Checkpoint
