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

package loi.asset.file.image.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.de.web.MediaType
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.AssetExtractor
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.index.{AssetDataDocument, HtmlExtractor}
import loi.authoring.syntax.index.*
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import javax.validation.constraints.Size

/** The image asset.
  */
final case class Image(
  // this should have @NotEmpty, but old data will blow up otherwise
  title: String = "[no title]",
  @Size(min = 0, max = 255)
  subtitle: String = "",
  archived: Boolean = false,
  @Size(min = 0, max = 255)
  keywords: String = "",
  altText: Option[String] = None,
  caption: Option[String] = None,
  width: Long = 0L,
  height: Long = 0L,
  mimeType: String = "",
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  source: Option[BlobRef] = None
)

object Image:

  implicit val assetTypeForImage: AssetType[Image] = new AssetType[Image](AssetTypeId.Image):
    override val allowedAttachmentTypes: Set[MediaType] = Set(
      MediaType.IMAGE_JPEG,
      MediaType.IMAGE_GIF,
      MediaType.IMAGE_PNG,
      MediaType.IMAGE_BMP,
      MediaType.IMAGE_WEBP,
      MediaType.APPLICATION_SVG
    )

    // title should have @NotEmpty but old data will blow up
    override def validate(data: Image): ValidatedNel[String, Unit] =
      Validate.size("subtitle", max = 255)(data.subtitle) *>
        Validate.size("keywords", max = 255)(data.keywords)

    // ignore mimeType as duplicative of contentType
    // ignore source content
    override def index(a: Image)(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument =
      AssetDataDocument(
        title = a.title.option,
        subtitle = a.subtitle.option,
        keywords = a.keywords.option,
        license = a.license,
        author = a.author,
        attribution = a.attribution,
        fileName = a.source.map(_.filename),
        contentType = a.source.map(_.contentType),
        content = stringifyOpt(Seq(a.caption.map(HtmlExtractor.fromHtml), a.altText))
      )

    override def htmls(data: Image)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      data.caption.toList

  object Asset extends AssetExtractor[Image]
end Image
