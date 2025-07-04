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
import com.fasterxml.jackson.annotation.JsonProperty
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

import java.math.BigDecimal
import java.util.UUID
import javax.validation.constraints.{Min, Size}

final case class Assignment1(
  @Size(min = 1, max = 255)
  title: String,
  iconAlt: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-design",
  @JsonProperty("prompt")
  instructions: BlockPart = BlockPart(Seq(HtmlPart())),
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  @Min(value = 1L)
  maxAttempts: Option[Long] = None,
  unlimitedAttempts: Boolean = false,
  scoringOption: Option[ScoringOption] = Some(ScoringOption.MostRecentAttemptScore),
  assessmentType: AssessmentType = AssessmentType.Formative,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  isForCredit: Boolean = false,
  @Min(value = 1L)
  pointsPossible: BigDecimal = BigDecimal.valueOf(100.0),
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
  @Min(value = 0L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  duration: Option[Long] = None,
)

object Assignment1:

  implicit val assetTypeForAssignment1: AssetType[Assignment1] = new AssetType[Assignment1](AssetTypeId.Assignment):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.Assesses          -> AssetTypeId.CompetencyTypes,
      Group.CblRubric         -> Set(AssetTypeId.Rubric),
      Group.Gates             -> AssetTypeId.GatedTypes,
      Group.Resources         -> AssetTypeId.FileTypes,
      Group.GradebookCategory -> Set(AssetTypeId.GradebookCategory),
      Group.Hyperlinks        -> AssetTypeId.HyperlinkTypes,
      Group.Survey            -> Set(AssetTypeId.Survey1),
    )

    override def validate(data: Assignment1): ValidatedNel[String, Unit] =
      Validate.size("title", min = 1, max = 255)(data.title) *>
        Validate.size("keywords", max = 255)(data.keywords) *>
        Validate.min("maxAttempts", 1)(data.maxAttempts) *>
        Validate.min("pointsPossible", 1)(data.pointsPossible)
    override def edgeIds(a: Assignment1): Set[UUID]                      = Option(a.instructions).map(_.edgeIds).getOrElse(Set.empty)

    override def render(a: Assignment1, targets: Map[UUID, Asset[?]]): Assignment1 =
      a.copy(instructions = Option(a.instructions).map(_.render(targets)).orNull)

    override def index(
      data: Assignment1
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument =
      AssetDataDocument(
        title = data.title.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        instructions = stringifyOpt(data.instructions)
      )

    override def htmls(
      data: Assignment1
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      data.instructions.htmls

  object Asset extends AssetExtractor[Assignment1]
end Assignment1
