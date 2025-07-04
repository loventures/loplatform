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
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.de.web.MediaType
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.AssetExtractor
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.index.AssetDataDocument
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import javax.validation.constraints.Size

final case class Javascript(
  @Size(min = 1, max = 255)
  title: String,
  @Size(min = 0, max = 255)
  subtitle: String = "",
  archived: Boolean = false,
  @Size(min = 0, max = 255)
  keywords: String = "",
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  source: Option[BlobRef] = None,
)

object Javascript:

  implicit val assetTypeForJavascript: AssetType[Javascript] =
    new AssetType[Javascript](AssetTypeId.Javascript):
      override val allowedAttachmentTypes: Set[MediaType] =
        Set(MediaType.APPLICATION_JAVASCRIPT, MediaType.TEXT_JAVASCRIPT)

      override def validate(data: Javascript): ValidatedNel[String, Unit] =
        Validate.size("title", 1, 255)(data.title) *>
          Validate.size("subtitle", max = 255)(data.subtitle) *>
          Validate.size("keywords", max = 255)(data.keywords)

      // ignore source content
      override def index(
        data: Javascript
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = data.title.option,
        subtitle = data.subtitle.option,
        keywords = data.keywords.option,
        license = data.license,
        author = data.author,
        attribution = data.attribution,
        fileName = data.source.map(_.filename),
        contentType = data.source.map(_.contentType),
      )

      override def htmls(
        data: Javascript
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] = Nil

  object Asset extends AssetExtractor[Javascript]
end Javascript
