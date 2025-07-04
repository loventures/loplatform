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

package loi.asset.file.pdf.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.de.web.MediaType
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.AssetExtractor
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.index.AssetDataDocument
import loi.authoring.syntax.index.*
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import javax.validation.constraints.Size

final case class Pdf(
  @Size(min = 1, max = 255)
  title: String = "[no title]",
  @Size(min = 0, max = 255)
  subtitle: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  mimeType: String = "",
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  source: Option[BlobRef] = None,
)

object Pdf:

  implicit val assetTypeForPdfData: AssetType[Pdf] =
    new AssetType[Pdf](AssetTypeId.Pdf):
      override val allowedAttachmentTypes: Set[MediaType] = Set(MediaType.APPLICATION_PDF)

      override def validate(data: Pdf): ValidatedNel[String, Unit] =
        Validate.size("title", 1, 255)(data.title) *>
          Validate.size("subtitle", max = 255)(data.subtitle) *>
          Validate.size("keywords", max = 255)(data.keywords)

      // ignore a.mimeType as duplicative of contentType
      override def index(a: Pdf)(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument =
        AssetDataDocument(
          title = a.title.option,
          subtitle = a.subtitle.option,
          keywords = a.keywords.option,
          license = a.license,
          author = a.author,
          attribution = a.attribution,
          fileName = a.source.map(_.filename),
          contentType = a.source.map(_.contentType),
          content = a.source.map(stringify(_))
        )

      override def htmls(data: Pdf)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        Nil

  object Asset extends AssetExtractor[Pdf]
end Pdf
