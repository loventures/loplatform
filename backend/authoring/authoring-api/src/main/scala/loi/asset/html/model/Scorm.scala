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

package loi.asset.html.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.de.web.MediaType
import loi.asset.file.fileBundle.model.FileBundle
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.validate.Validate
import scalaz.std.list.*
import scaloi.syntax.option.*

import java.math.BigDecimal
import javax.validation.constraints.{Min, Size}

//This represents a SCORM Activity asset, which utilizes an upload of a SCORM zip file
final case class Scorm(
  @Size(min = 1, max = 255)
  title: String,
  iconAlt: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-book",
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  @Min(value = 1L)
  duration: Option[Long] = None,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  @Min(value = 1L)
  contentHeight: Option[Long] = None,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  @Min(value = 1L)
  contentWidth: Option[Long] = None,
  launchNewWindow: Boolean = false,
  zipPaths: Seq[String],
  scormTitle: Option[String],       // title found inside scorm's imsmanifest.xml
  resourcePath: String,             // main resource href as found inside scorm's imsmanifest.xml (i.e. something/index.html)
  allRefs: Map[String, String],     // map of all item/resource identifiers -> identifierref/hrefs, from imsmanifest.xml
  passingScore: Option[BigDecimal], // threshold to be considered passing from the imsmanifest.xml
  objectiveIds: Seq[String],        // unique list of `primaryObjective` and `objective` elements in the imsmanifest.xml
  sharedDataIds: Seq[String],       // unique list of IDs for shared data, referenceable between multiple SCORM activities
  isForCredit: Boolean = false,
  @Min(value = 1L)
  pointsPossible: BigDecimal = BigDecimal.valueOf(100.0),
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  source: Option[BlobRef] = None,
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
)

object Scorm:

  implicit val assetTypeForScorm: AssetType[Scorm] =
    new AssetType[Scorm](AssetTypeId.Scorm):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Teaches           -> AssetTypeId.CompetencyTypes,
        Group.GradebookCategory -> Set(AssetTypeId.GradebookCategory),
        Group.Hyperlinks        -> AssetTypeId.HyperlinkTypes,
        Group.Survey            -> Set(AssetTypeId.Survey1),
      )

      override val allowedAttachmentTypes: Set[MediaType] =
        Set(MediaType.APPLICATION_ZIP, MediaType.APPLICATION_ZIP_COMPRESSED)

      override def validate(data: Scorm): ValidatedNel[String, Unit] =
        Validate.size("title", 1, 255)(data.title) *>
          Validate.size("keywords", max = 255)(data.keywords) *>
          Validate.min("duration", 0)(data.duration) *>
          Validate.min("pointsPossible", 1)(data.pointsPossible)

      // Ideally this would result in multiple index documents, one per indexed file, so results could point to where
      // the match occurred. However, the comparative paucity of file bundle argues against making such efforts.
      override def index(
        a: Scorm
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = a.title.option,
        keywords = a.keywords.option,
        license = a.license,
        author = a.author,
        attribution = a.attribution,
        content = a.source.map(FileBundle.fileBundleText)
      )

      override def htmls(data: Scorm)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.source.foldZ(FileBundle.fileBundleHtmls)

  object Asset extends AssetExtractor[Scorm]
end Scorm
