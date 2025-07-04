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

import com.google.common.hash.{HashCode, Hashing}
import com.google.common.io.ByteSource
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.attachment.{
  AttachmentProvider,
  Disposition,
  AttachmentService as CpxpAttachmentService
}
import com.learningobjects.cpxp.service.blob.BlobPutLocation
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.util.BlobInfo
import com.learningobjects.de.web.MediaType
import com.typesafe.config.Config
import loi.asset.blob.SourceProperty
import loi.asset.file.videoCaption.model.VideoCaption
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.blob.exception.*
import org.apache.commons.codec.digest.DigestUtils
import org.jclouds.blobstore.domain.Blob
import scalaz.syntax.std.option.*

import java.io.InputStream
import java.net.URI
import scala.annotation.{nowarn, tailrec}
import scala.util.{Failure, Success, Try, Using}

@Service
class BaseBlobService(
  cpxpAttachmentService: CpxpAttachmentService,
  typesafeConfig: Config,
  domainDTO: => DomainDTO
) extends BlobService:

  // Validates supported media types
  // todo: remove after simplifying blobstore APIs
  def createPutUrl(
    blobName: String,
    contentType: MediaType,
    contentLength: Long,
    assetType: AssetType[?],
  ): Try[BlobPutLocation] =
    for provider <- validateBlobData(getDefaultProvider, blobName, contentType, assetType, None)
    yield
      if provider.isS3 then
        provider.buildPutUrl(
          blobName,
          contentType,
          contentLength,
          typesafeConfig.getDuration("loi.authoring.blob.signedUrlValidDuration").getSeconds
        )
      else buildLocalPutUrl(provider, blobName)

  // Does not care about specific media types
  override def createPutUrl(
    blobName: String,
    contentType: MediaType,
    contentLength: Long,
    exemptBlobName: Option[String],
    provider: Option[AttachmentProvider] = None,
  ): Try[BlobPutLocation] =
    for provider <- validateBlobData(provider | getDefaultProvider, blobName, exemptBlobName)
    yield
      if provider.isS3 then
        provider.buildPutUrl(
          blobName,
          contentType,
          contentLength,
          typesafeConfig.getDuration("loi.authoring.blob.signedUrlValidDuration").getSeconds
        )
      else buildLocalPutUrl(provider, blobName)

  private def buildLocalPutUrl(
    provider: AttachmentProvider,
    blobName: String,
  ): BlobPutLocation =
    val endpoint = new URI(s"/api/v2/authoring/attachment?name=$blobName")
    BlobPutLocation(endpoint, provider.name)

  override def putBlob(
    provider: AttachmentProvider,
    blobName: String,
    uploadInfo: UploadInfo
  ): Try[BlobRef] =
    for (_ <- validateBlobData(provider, blobName, Some(blobName))) yield
      val ref = BlobRef(
        provider.name,
        blobName,
        uploadInfo.getFileName,
        uploadInfo.getMediaType.get,
        uploadInfo.getSize
      )
      if provider.blobExists(blobName) then ref
      else
        provider.putBlob(blobName, uploadInfo.getFile)
        ref

  override def putBlob(
    provider: AttachmentProvider,
    blobName: String,
    filename: String,
    mediaType: MediaType,
    blobSize: Long,
    source: ByteSource
  ): Try[BlobRef] =
    for (_ <- validateBlobData(provider, blobName, Some(blobName))) yield
      val ref = BlobRef(provider.name, blobName, filename, mediaType, blobSize)
      if provider.blobExists(blobName) then ref
      else
        provider.putBlob(blobName, source)
        ref

  override def getBlobMd5(
    blobRef: BlobRef,
  ): HashCode =
    val provider = getProvider(blobRef.provider)
    val blob     = provider.getBlob(blobRef.name)
    val md5Opt   = Option(blob.getMetadata.getContentMetadata.getContentMD5AsHashCode)
    md5Opt.getOrElse(hash(blob))

  private def hash(blob: Blob): HashCode =
    @nowarn // md5 deprecated
    val hasher = Hashing.md5.newHasher
    val buffer = new Array[Byte](8192)
    Using.resource(blob.getPayload.openStream) { in =>
      @tailrec def loop(amt: Int): Unit =
        if amt >= 0 then
          hasher.putBytes(buffer, 0, amt)
          loop(in.read(buffer))
      loop(in.read(buffer))
    }
    hasher.hash
  end hash

  override def validateBlobData(
    provider: AttachmentProvider,
    blobName: String,
    contentType: MediaType,
    assetType: AssetType[?],
    exemptBlobName: Option[String]
  ): Try[AttachmentProvider] =

    for
      _ <- checkMediaType(contentType, assetType)
      _ <- checkBlobName(blobName, exemptBlobName)
    yield provider

  private def validateBlobData(
    provider: AttachmentProvider,
    blobName: String,
    exemptBlobName: Option[String]
  ): Try[AttachmentProvider] =
    for (_ <- checkBlobName(blobName, exemptBlobName))
      yield provider

  def checkMediaType(
    mediaType: MediaType,
    assetType: AssetType[?]
  ): Try[Unit] =
    if assetType.allowsAttachmentType(mediaType) then Success(())
    else Failure(IllegalMediaType(mediaType))

  override def validateBlobData(
    blobRef: BlobRef,
    exemptName: Option[String]
  ): Try[AttachmentProvider] =
    val provider = getProvider(blobRef.provider)
    validateBlobData(provider, blobRef.name, exemptName)

  override def blobExists(blobRef: BlobRef): Boolean =
    val provider = getProvider(blobRef.provider)
    provider.blobExists(blobRef.name)

  override def blobExists(
    blobName: String,
    provider: AttachmentProvider
  ): Boolean = provider.blobExists(blobName)

  private def checkBlobName(blobName: String, exemptBlobName: Option[String]): Try[Unit] =

    val isExempt              = exemptBlobName.exists(_.equals(blobName))
    lazy val startsWithPrefix =
      val requiredNamePrefix = typesafeConfig.getString("loi.authoring.blob.requiredNamePrefix")
      blobName.startsWith(requiredNamePrefix) && blobName.length > requiredNamePrefix.length

    if isExempt || startsWithPrefix || blobName.startsWith("resource.2/") then Success(())
    else Failure(IllegalBlobName(blobName))

  override def getBlobInfo(node: Asset[?]): Option[BlobInfo] =
    SourceProperty
      .fromNode(node)
      .map(src =>
        val info = ref2Info(src)
        info.setNoRedirect(setBlobNoRedirect(node))
        info
      )

  override def ref2Info(ref: BlobRef, disposition: Disposition = Disposition.inline): BlobInfo =
    val provider = getProvider(ref.provider)
    val blobInfo = new BlobInfo(provider.getBlob(ref.name), provider, ref.filename, ref.name, ref.size)
    blobInfo.setContentType(ref.contentType.toString)
    blobInfo.setDisposition(disposition, ref.filename)
    blobInfo

  // There is a bug in chrome that causes Origin to be set to null when it requests
  // fonts or video captions.  When that is null the request is failed since it does not
  // satisfy the cross origin check.  For these media types, the file will be downloaded
  // via DE rather than being redirected to an S3 url.
  private def setBlobNoRedirect(node: Asset[?]): Boolean =
    node match
      case VideoCaption.Asset(_) => true
      case _                     => false

  override def getDefaultProvider: AttachmentProvider =
    cpxpAttachmentService.getDefaultProvider

  override def getProvider(name: String): AttachmentProvider =
    Try {
      cpxpAttachmentService.getProvider(name)
    }.recover({ case ex: IllegalArgumentException =>
      throw NoSuchProvider(name, ex)
    }).get

  override def dedupBlobName(path: String, filename: String, provider: AttachmentProvider): String =
    val dot    = filename.lastIndexOf('.')
    val prefix = if dot < 0 then filename else filename.substring(0, dot)
    val suffix = if dot < 0 then "" else filename.substring(dot)

    // Until we ditch jclouds just do this inefficiently, we should just list files beginning with $path$prefix
    def loop(num: Int): String =
      val proposed = s"$prefix ($num)$suffix"
      if provider.blobExists(s"$path/$proposed") then loop(1 + num) else proposed
    if provider.blobExists(s"$path/$filename") then loop(1) else filename
  end dedupBlobName

  override def createBlobName(
    is: InputStream,
    prefix: String
  ): String =
    val digest = DigestUtils.md5Hex(is)
    s"$prefix${domainDTO.id}/${digest.substring(0, 1)}/${digest.substring(1, 2)}/${digest.substring(2, 3)}/${digest.substring(3)}"

  override def ref2Stream(
    blobRef: BlobRef
  ): InputStream =
    val blob = ref2Blob(blobRef)
    blob.getPayload.openStream()

  private def ref2Blob(
    blobRef: BlobRef
  ): Blob =
    val provider = getProvider(blobRef.provider)
    provider.getBlob(blobRef.name)
end BaseBlobService
