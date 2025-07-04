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

package loi.authoring.blob

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.attachment.AttachmentService
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.de.web.MediaType
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.apache.tika.Tika
import scala.util.Using
import scalaz.std.string.*
import scalaz.syntax.apply.*
import scalaz.syntax.foldable.*
import scalaz.syntax.std.option.*
import scalaz.syntax.validation.*
import scalaz.{Validation, ValidationNel}
import scaloi.syntax.`try`.*

import scala.util.Try

@Service
class BlobRefConvertService(
  attachmentService: AttachmentService,
  mimeService: MimeWebService,
  xa: => Transactor[IO]
):
  import scalaz.Validation.FlatMap.*

  import BlobRefConvertService.*

  /** Gets the [[BlobRef]] s for the given attachment ids. Map is attachment id to possible blobref. The validation
    * represents either the successful `BlobRef` or a failure that would look like "null provider; null digest"
    * @param attachmentIds
    * @return
    */
  def attachmentId2BlobRef(attachmentIds: Seq[Long]): Map[Long, Validation[String, BlobRef]] =
    val projs = sql"""
      select id, provider, root_id, digest, filename, size
      from attachmentfinder
      where id = any(${attachmentIds.toList})
      """
      .query[AttProjection]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
    projs.map(proj2Ref).toMap
  end attachmentId2BlobRef

  private def proj2Ref(proj: AttProjection): (Long, Validation[String, BlobRef]) =
    proj match
      case (id, provider, rootId, digest, filename, size) =>
        val validatedBlobRef = (
          provider.toSuccessNel("null provider") |@|
            digest.toSuccessNel("null digest").map(digest2BlobName(rootId)) |@|
            validatedFilenameAndMediaType(id, filename) |@|
            size.toSuccessNel("null size")
        ) { case (p, name, (fname, mtype), s) =>
          BlobRef(p, name, fname, mtype, s)
        }

        // since the string elements of the NonEmptyList are so short,
        // its easier to read the errors as one string like this
        id -> validatedBlobRef
          .leftMap(errors => s"bad attachment $id: " + errors.intercalate("; "))

  private def digest2BlobName(rootId: Long)(d: String): String =
    s"$rootId/${d.substring(0, 1)}/${d.substring(1, 2)}/${d.substring(2, 3)}/${d.substring(3)}"

  private def validatedFilenameAndMediaType(
    attachmentId: Long,
    filename: Option[String]
  ): ValidationNel[String, (String, MediaType)] =
    for
      fn <- filename.toSuccessNel("null filename")
      mt <- mediaType(attachmentId, fn)
    yield (fn, mt)

  private def mediaType(attachId: Long, filename: String): ValMediaType =
    typeFromName(filename)
      .map(_.successNel[String])
      .getOrElse(typeFromFile(attachId, filename))

  private def typeFromName(filename: String): Option[MediaType] =
    // clean up instances where the extension was derived from a URL with parameters
    // (e.g. ".eot#iefix", 43 instances of this in prod)
    val ext = FilenameUtils.getExtension(filename).replaceFirst("[^\\w].+$", "")
    if StringUtils.isNotEmpty(ext) then
      val fn       = filename.substring(0, filename.lastIndexOf(ext) - 1) +
        FilenameUtils.EXTENSION_SEPARATOR + ext
      val mimeType = mimeService.getMimeType(fn)
      Try(MediaType.parseMediaType(mimeType)).tapFailure { e =>
        log.info(s"error parsing media type fn=$fn, mimeType=$mimeType: ${e.getMessage}")
      }.toOption
    else None
  end typeFromName

  private lazy val tika                                                    = new Tika()
  private def typeFromFile(attachId: Long, filename: String): ValMediaType =
    log.info(s"attempting to find media type of attachment[$attachId] from binary data")
    val ctx = s"attachment[$attachId], fn=$filename"
    (for
      aItem     <- Option(attachmentService.getAttachment(attachId)) \/>
                     s"could not load attachment $ctx"
      info      <- Option(attachmentService.getAttachmentBlob(aItem)) \/>
                     s"could not load blob info $ctx"
      mimeType  <- Using.resource(info.openInputStream()) { is =>
                     Try(tika.detect(is, filename))
                       .tapFailure(e => log.warn(e)(s"error detecting mimeType $ctx"))
                       .toOption \/> s"tika error $ctx"
                   }
      err        = s"error parsing media type mimeType=$mimeType, $ctx"
      mediaType <- Try(MediaType.parseMediaType(mimeType))
                     .tapFailure(e => log.info(s"$err: ${e.getMessage}"))
                     .toOption \/> s"$err"
    yield mediaType).toValidationNel
  end typeFromFile
end BlobRefConvertService

private object BlobRefConvertService:
  private type AttProjection = (Long, Option[String], Long, Option[String], Option[String], Option[Long])
  private type ValMediaType  = ValidationNel[String, MediaType]
  private val log = org.log4s.getLogger
