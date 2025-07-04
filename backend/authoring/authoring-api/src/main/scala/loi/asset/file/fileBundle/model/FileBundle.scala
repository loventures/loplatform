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

package loi.asset.file.fileBundle.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.de.web.MediaType
import loi.asset.fileBundle.model.DisplayFileRenderChoice
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.edge.Group
import loi.authoring.index.BlobExtractor.*
import loi.authoring.index.{AssetDataDocument, BlobExtractor}
import loi.authoring.validate.Validate
import org.apache.commons.io.input.CloseShieldInputStream
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.std.stream.unfold
import scalaz.syntax.std.option.*
import scaloi.syntax.functor.*
import scaloi.syntax.option.*

import java.io.{BufferedInputStream, InputStream}
import java.util.zip.ZipInputStream
import javax.validation.constraints.{Min, Size}

// TODO: is this the right name
final case class FileBundle(
  @Size(min = 1, max = 255)
  title: String = "[no title]",
  @Size(min = 0, max = 255)
  subtitle: String = "",
  iconAlt: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-book",
  @Min(value = 0L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  duration: Option[Long] = None,
  displayFiles: Seq[DisplayFile] = Seq.empty,
  zipFileTree: Seq[ZipFileTree] = Seq.empty,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  source: Option[BlobRef] = None,
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
)

object FileBundle:

  implicit val assetTypeForFileBundle: AssetType[FileBundle] =
    new AssetType[FileBundle](AssetTypeId.FileBundle):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Teaches    -> AssetTypeId.CompetencyTypes,
        Group.Hyperlinks -> AssetTypeId.HyperlinkTypes,
        Group.Survey     -> Set(AssetTypeId.Survey1),
      )

      override val allowedAttachmentTypes: Set[MediaType] =
        Set(MediaType.APPLICATION_ZIP, MediaType.APPLICATION_ZIP_COMPRESSED)

      override def validate(data: FileBundle): ValidatedNel[String, Unit] =
        Validate.size("title", 1, 255)(data.title) *>
          Validate.size("subtitle", max = 255)(data.subtitle) *>
          Validate.size("keywords", max = 255)(data.keywords) *>
          Validate.min("duration", 0)(data.duration)

      // Ideally this would result in multiple index documents, one per indexed file, so results could point to where
      // the match occurred. However, the comparative paucity of file bundle argues against making such efforts.
      override def index(
        a: FileBundle
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        title = a.title.option,
        subtitle = a.subtitle.option,
        keywords = a.keywords.option,
        license = a.license,
        author = a.author,
        attribution = a.attribution,
        content = a.source.map(fileBundleText)
      )

      override def htmls(
        data: FileBundle
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.source.foldZ(fileBundleHtmls)

  def fileBundleText(
    blob: BlobRef
  )(implicit blobService: BlobService, mimeWebService: MimeWebService): String =
    fileBundleExtract(blob, BlobExtractor.blobStrings(_)).flatten.mkString(" ")

  def fileBundleHtmls(
    blob: BlobRef
  )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
    fileBundleExtract(blob, BlobExtractor.blobHtml(_)).flatten

  private def fileBundleExtract[A](
    blob: BlobRef,
    extract: FileBundleBlob => A
  )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[A] =
    BlobExtractor
      .extractRaw(
        blob,
        in =>
          unfold(new ZipInputStream(in))(zis =>
            zis.getNextEntry.option.map { entry =>
              val contentType = Option(mimeWebService.getMimeType(entry.getName))
                .map(MediaType.parseMediaType) | MediaType.APPLICATION_UNKNOWN
              extract(FileBundleBlob(blob, entry.getName, contentType, entry.getSize, zis))
            } <*- zis
          ).toList
      )
      .getOrElse(Nil)

  /** Wrap a file bundle entry. */
  final case class FileBundleBlob(zip: BlobRef, fileName: String, contentType: MediaType, size: Long, in: InputStream)

  object FileBundleBlob:
    implicit def fileBundleBlobLike: BlobLike[FileBundleBlob] =
      new BlobLike[FileBundleBlob]:
        override def toString(a: FileBundleBlob): String       = s"FileBundleBlob[${a.zip}/${a.fileName}]"
        override def fileName(a: FileBundleBlob): String       = a.fileName
        override def contentType(a: FileBundleBlob): MediaType = a.contentType
        override def size(a: FileBundleBlob): Long             = a.size
        override def open(a: FileBundleBlob): InputStream      = new BufferedInputStream(CloseShieldInputStream.wrap(a.in))

  object Asset extends AssetExtractor[FileBundle]
end FileBundle

case class DisplayFile(
  path: String,
  mimeType: Option[String] = None,
  displayName: String,
  renderChoice: DisplayFileRenderChoice
)

case class ZipFileTree(
  title: String,
  path: String,
  nodes: Seq[ZipFileTree]
)
