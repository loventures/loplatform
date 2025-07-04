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

package loi.asset.discussion.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.assessment.model.AssessmentType
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.*
import loi.authoring.asset.factory.*
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.syntax.index.*
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import java.math.BigDecimal
import java.util.UUID
import javax.validation.constraints.{Min, Size}

//TODO: hopefully we'll be able to transition the rest of the instructions away from legacy content and update the property
final case class Discussion1(
  @Size(min = 1, max = 255)
  title: String,
  iconAlt: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-bubbles",
  @JsonProperty("instructionsBlock")
  instructions: BlockPart = BlockPart(Seq(HtmlPart())),
  @JsonProperty("instructions")
  legacyInstructions: HtmlPart = HtmlPart(),
  @deprecatedField()
  viewBeforePosting: Option[Boolean] = Some(true),
  @JsonProperty("allowMultiplePosting")
  @deprecatedField()
  shouldAllowMultiplePosting: Option[Boolean] = Some(true),
  gradable: Boolean = false,
  anonymized: Boolean = false,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  @Min(value = 1L)
  duration: Option[Long] = None,
  assessmentType: AssessmentType = AssessmentType.Formative,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  isForCredit: Boolean = false,
  @Min(value = 1L)
  pointsPossible: BigDecimal = BigDecimal.valueOf(100.0),
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
)

object Discussion1:

  implicit val assetTypeForDiscussion1: AssetType[Discussion1] = new AssetType[Discussion1](AssetTypeId.Discussion):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.Assesses          -> AssetTypeId.CompetencyTypes,
      Group.CblRubric         -> Set(AssetTypeId.Rubric),
      Group.Resources         -> AssetTypeId.FileTypes,
      Group.GradebookCategory -> Set(AssetTypeId.GradebookCategory),
      Group.Hyperlinks        -> AssetTypeId.HyperlinkTypes,
      Group.Survey            -> Set(AssetTypeId.Survey1),
    )

    override def validate(data: Discussion1): ValidatedNel[String, Unit] =
      Validate.size("title", 1, 255)(data.title) *>
        Validate.size("keywords", max = 255)(data.keywords) *>
        Validate.min("duration", 1)(data.duration) *>
        Validate.min("pointsPossible", 1)(data.pointsPossible)

    override def edgeIds(data: Discussion1): Set[UUID] =
      Option(data.instructions).map(_.edgeIds).getOrElse(Set.empty) ++
        Option(data.legacyInstructions).map(_.edgeIds).getOrElse(Set.empty)

    override def render(data: Discussion1, targets: Map[UUID, Asset[?]]): Discussion1 =
      val renderedInstructions       = Option(data.instructions).map(_.render(targets)).orNull
      val renderedLegacyInstructions = Option(data.legacyInstructions).map(_.render(targets)).orNull
      data.copy(instructions = renderedInstructions, legacyInstructions = renderedLegacyInstructions)

    // data.legacyInstructions appear unused
    override def index(
      data: Discussion1
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
      data: Discussion1
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      data.instructions.htmls

  object Asset extends AssetExtractor[Discussion1]
end Discussion1
