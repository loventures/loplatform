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

package loi.cp.blob.s3

import com.learningobjects.cpxp.util.GuidUtil.longGuid
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*

import java.io.File
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using.Releasable

case class MultipartUpload(cli: S3Client, bucket: String, blobName: String):
  val key: String                                  = longGuid
  private val parts: mutable.Buffer[CompletedPart] = mutable.ListBuffer() // sorry
  private var uploadId: String                     = scala.compiletime.uninitialized

  def open(): Unit =
    uploadId = cli
      .createMultipartUpload(CreateMultipartUploadRequest.builder.bucket(bucket).key(blobName).build)
      .uploadId

  // I feel this is garbage, is it actually paging through the file?
  def addPart(src: File): Unit =
    val partNumber = parts.size + 1
    val part       = UploadPartRequest.builder
      .bucket(bucket)
      .key(blobName)
      .uploadId(uploadId)
      .partNumber(partNumber)
      .build
    val response   = cli.uploadPart(part, src.toPath)
    parts += CompletedPart.builder.partNumber(partNumber).eTag(response.eTag).build
end MultipartUpload

object MultipartUpload:
  implicit val multipartUploadResource: Releasable[MultipartUpload] = (upl: MultipartUpload) =>
    upl.cli.completeMultipartUpload(
      CompleteMultipartUploadRequest.builder
        .bucket(upl.bucket)
        .key(upl.blobName)
        .uploadId(upl.uploadId)
        .multipartUpload(
          CompletedMultipartUpload.builder.parts(upl.parts.asJava).build
        )
        .build,
    )
end MultipartUpload
