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

final case class Assessment(
  @Size(min = 1, max = 255)
  title: String,
  iconAlt: String = "",
  @Size(min = 0, max = 255)
  subtitle: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-text",
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  @Min(value = 0L) // because `unlimitedAttempts` = 0 and this asset type doesn't have a separate boolean flag for it
  maxAttempts: Option[Long] = None,
  @Min(value = 1L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  maxMinutes: Option[Long] = None,
  name: Option[String] = None,
  singlePage: Boolean = false,
  immediateFeedback: Boolean = false,
  @JsonProperty("hideAnswerIfIncorrect")
  shouldHideAnswerIfIncorrect: Boolean = false,
  @JsonProperty("displayConfidenceIndicators")
  shouldDisplayConfidenceIndicator: Option[Boolean] = Some(true),
  @JsonProperty("randomizeQuestionOrder")
  shouldRandomizeQuestionOrder: Boolean = false,
  instructions: BlockPart = BlockPart(Seq(HtmlPart())),
  scoringOption: Option[ScoringOption] = Some(ScoringOption.MostRecentAttemptScore),
  assessmentType: AssessmentType = AssessmentType.Formative,
  license: Option[License] = None,
  @Size(min = 0, max = 255)
  author: Option[String] = None,
  @Size(min = 0, max = 255)
  attribution: Option[String] = None,
  @Min(value = 1L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  softAttemptLimit: Option[Long] = None,
  @Size(min = 0, max = 255)
  softLimitMessage: Option[String] = None,
  isForCredit: Boolean = false,
  @Min(value = 1L)
  pointsPossible: BigDecimal = BigDecimal.valueOf(100.0),
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
  @Min(value = 0L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  duration: Option[Long] = None,
)

object Assessment:
  implicit val assetTypeForAssessment: AssetType[Assessment] = new AssetType[Assessment](AssetTypeId.Assessment):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.Gates             -> AssetTypeId.GatedTypes,
      Group.Questions         -> AssetTypeId.QuestionTypes,
      Group.Resources         -> AssetTypeId.FileTypes,
      Group.GradebookCategory -> Set(AssetTypeId.GradebookCategory),
      Group.Hyperlinks        -> AssetTypeId.HyperlinkTypes,
      Group.Survey            -> Set(AssetTypeId.Survey1),
    )

    override def validate(data: Assessment): ValidatedNel[String, Unit] =
      Validate.size("title", min = 1, max = 255)(data.title) *>
        Validate.size("subtitle", max = 255)(data.subtitle) *>
        Validate.size("keywords", max = 255)(data.keywords) *>
        Validate.min("maxAttempts", 0)(data.maxAttempts) *>
        Validate.min("maxMinutes", 1)(data.maxMinutes) *>
        Validate.size("author", max = 255)(data.author) *>
        Validate.size("attribution", max = 255)(data.attribution) *>
        Validate.min("softAttemptLimit", 1)(data.softAttemptLimit) *>
        Validate.size("softLimitMessage", max = 255)(data.softLimitMessage) *>
        Validate.min("pointsPossible", 1)(data.pointsPossible)

    override def edgeIds(a: Assessment): Set[UUID] = Option(a.instructions).map(_.edgeIds).getOrElse(Set.empty)

    override def render(a: Assessment, targets: Map[UUID, Asset[?]]): Assessment =
      a.copy(instructions = Option(a.instructions).map(_.render(targets)).orNull)

    // data.name appears unused
    override def index(
      data: Assessment
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
      data: Assessment
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      data.instructions.htmls

  object Asset extends AssetExtractor[Assessment]
end Assessment
