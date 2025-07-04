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

import com.google.common.hash.HashCode
import com.google.common.io.ByteSource
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.attachment.{AttachmentProvider, Disposition}
import com.learningobjects.cpxp.service.blob.BlobPutLocation
import com.learningobjects.cpxp.util.BlobInfo
import com.learningobjects.de.web.MediaType
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.blob.exception.NoSuchBlobRef
import scaloi.syntax.OptionOps.*

import java.io.InputStream
import scala.util.Try

@Service
trait BlobService:

  /** @param assetType
    *   Used to check whether the blob data is a valid type with this node.
    * @param exemptBlobName
    *   old blob names didn't start with the authoring/ prefix and they're allowed to stay that way. Thus depending on
    *   why the caller is validating, they may want to pass the old name if they have it. A caller that is creating a
    *   new blob should never sent it though. It should get with the times.
    */
  def validateBlobData(
    provider: AttachmentProvider,
    blobName: String,
    contentType: MediaType,
    assetType: AssetType[?],
    exemptBlobName: Option[String]
  ): Try[AttachmentProvider]

  /** Unlike the above function, here there is no check with a `nodeConfig`.
    */
  def validateBlobData(
    blobRef: BlobRef,
    exemptBlobName: Option[String]
  ): Try[AttachmentProvider]

  def blobExists(
    blobRef: BlobRef
  ): Boolean

  def blobExists(
    blobName: String,
    provider: AttachmentProvider
  ): Boolean

  /** Validates blob name and then, if it's not already in the blobstore, puts [[UploadInfo]] there. todo: return a
    * [[BlobRef]] without a [[Try]]
    */
  def putBlob(
    provider: AttachmentProvider,
    blobName: String,
    uploadInfo: UploadInfo
  ): Try[BlobRef]

  def putBlob(
    provider: AttachmentProvider,
    blobName: String,
    filename: String,
    mediaType: MediaType,
    blobSize: Long,
    source: ByteSource
  ): Try[BlobRef]

  def getBlobMd5(
    blobRef: BlobRef,
  ): HashCode

  /** Creates a PUT url for clients to use based on the specified provider or else the default.
    */
  def createPutUrl(
    blobName: String,
    contentType: MediaType,
    contentLength: Long,
    exemptBlobName: Option[String],
    provider: Option[AttachmentProvider] = None,
  ): Try[BlobPutLocation]

  /** @param node
    *   the node that might have a blob
    * @return
    *   the blob info of `node`
    */
  def getBlobInfo(node: Asset[?]): Option[BlobInfo]

  /** Gets the default provider
    *
    * @return
    *   the default provider or throws a IllegalArgumentException exception
    */
  def getDefaultProvider: AttachmentProvider

  /** @param name
    *   the provider name
    * @return
    *   the provider or throws a NoSuchProvider exception
    */
  def getProvider(name: String): AttachmentProvider

  /** Converts a BlobRef to a BlobInfo
    */
  def ref2Info(ref: BlobRef, disposition: Disposition = Disposition.inline): BlobInfo

  /** Like `getBlobInfo` but fails with a [[NoSuchBlobRef]] exception if `node` has no blob.
    *
    * @param node
    *   the node that might have a blob
    * @return
    *   the blob info of `node`
    */
  def requireBlobInfo(node: Asset[?]): Try[BlobInfo] =
    getBlobInfo(node).toTry(NoSuchBlobRef(node.info.id))

  /** Takes a proposed blobname and returns a name that does not conflict with any existing blob; for example,
    * foo/Bar.png might become foo/Bar (1).png.
    */
  def dedupBlobName(prefix: String, blobName: String, provider: AttachmentProvider): String

  /** Creates a blob name from an md5hash of the stream's bytes and a given prefix + domainId. Does NOT close the stream
    * for you.
    *
    * $prefix$domainId/$h/$a/$s/$h
    *
    * @param prefix
    *   can be an empty string, but otherwise you probably want to append with a slash, e.g., "authoring/"
    */
  def createBlobName(
    is: InputStream,
    prefix: String
  ): String

  /** Retrieves the blob from the blobstore and converts it to an open InputStream.
    */
  def ref2Stream(
    blobRef: BlobRef
  ): InputStream
end BlobService
