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

package loi.asset.external

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.service.mime.MimeWebService
import enumeratum.{ArgonautEnum, Enum, EnumEntry}
import loi.asset.contentpart.{BlockPart, HtmlPart}
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

import java.math.BigDecimal
import java.util.UUID
import javax.validation.constraints.{Min, Size}

final case class CourseLink(
  @Size(min = 1, max = 255)
  title: String,
  @Min(value = 1L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  duration: Option[Long] = None,
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  @Min(value = 1L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  branch: Option[Long] = None,
  newWindow: Boolean = false,
  sectionPolicy: SectionPolicy = SectionPolicy.MostRecent,
  instructions: BlockPart = BlockPart(Seq(HtmlPart())),
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  gradable: Boolean = false,
  isForCredit: Boolean = false,
  @Min(value = 1L)
  pointsPossible: BigDecimal = BigDecimal.valueOf(100.0),
  contentStatus: Option[String] = None,
)

object CourseLink:

  implicit val assetTypeForCourseLink: AssetType[CourseLink] =
    new AssetType[CourseLink](AssetTypeId.CourseLink):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Teaches           -> AssetTypeId.CompetencyTypes,
        Group.Assesses          -> AssetTypeId.CompetencyTypes,
        Group.GradebookCategory -> Set(AssetTypeId.GradebookCategory),
        Group.Hyperlinks        -> AssetTypeId.HyperlinkTypes,
        Group.Resources         -> AssetTypeId.FileTypes,
        Group.Gates             -> AssetTypeId.GatedTypes,
        Group.Survey            -> Set(AssetTypeId.Survey1),
      )

      override def validate(data: CourseLink): ValidatedNel[String, Unit] =
        Validate.size("title", 1, 255)(data.title) *>
          Validate.size("keywords", max = 255)(data.keywords)

      override def edgeIds(data: CourseLink): Set[UUID] =
        Option(data.instructions).map(_.edgeIds).getOrElse(Set.empty)

      override def render(data: CourseLink, targets: Map[UUID, Asset[?]]): CourseLink =
        data.copy(instructions = Option(data.instructions).map(_.render(targets)).orNull)

      override def index(
        data: CourseLink
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = data.title.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        instructions = stringifyOpt(data.instructions)
      )

      override def htmls(
        data: CourseLink
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] = data.instructions.htmls

  object Asset extends AssetExtractor[CourseLink]
end CourseLink

sealed trait SectionPolicy extends EnumEntry

object SectionPolicy extends Enum[SectionPolicy] with ArgonautEnum[SectionPolicy]:

  override val values = findValues

  case object MostRecent    extends SectionPolicy
  case object LinkedSection extends SectionPolicy
