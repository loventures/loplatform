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
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.syntax.index.*
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import javax.validation.constraints.{Min, Size}

final case class Html(
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
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  source: Option[BlobRef] = None,
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
)

object Html:

  implicit val assetTypeForHtml: AssetType[Html] =
    new AssetType[Html](AssetTypeId.Html):
      override val allowedAttachmentTypes: Set[MediaType] = Set(MediaType.TEXT_HTML)

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Dependencies -> Set(AssetTypeId.WebDependency),
        Group.Resources    -> AssetTypeId.FileTypes,
        Group.Resources    -> AssetTypeId.FileTypes,
        Group.Teaches      -> AssetTypeId.CompetencyTypes,
        Group.Survey       -> Set(AssetTypeId.Survey1),
        Group.Hyperlinks   -> AssetTypeId.HyperlinkTypes,
      )

      override def validate(data: Html): ValidatedNel[String, Unit] =
        Validate.size("title", 1, 255)(data.title) *>
          Validate.size("keywords", max = 255)(data.keywords) *>
          Validate.min("duration", 1)(data.duration)

      override def index(
        data: Html
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = data.title.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        content = data.source.map(stringify(_))
      )

      override def htmls(data: Html)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.source.htmls

  object Asset extends AssetExtractor[Html]
end Html
