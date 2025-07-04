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

package loi.authoring.exchange.imprt

import java.io.{File, FileInputStream}
import java.nio.file.Files
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.util.GuidUtil
import loi.authoring.blob.{BlobRef, BlobService}
import org.apache.commons.io.FileUtils
import scalaz.Validation
import scalaz.syntax.std.`try`.*

import scala.util.Using

/** Temporary internal helper service while we still rely on files for import rather than InputStream.
  */
@Service
class File2BlobService(blobService: BlobService):

  /** Creates a temp file whose path is its blobname.
    */
  def ref2TempFile(
    blobRef: BlobRef,
    suffix: String
  ): File =
    val cleanName = s"${blobRef.name.replaceAll("/", "_")}-${GuidUtil.longGuid}"
    val tempPath  = Files.createTempFile(cleanName, ".zip")
    val tempFile  = tempPath.toFile
    tempFile.deleteOnExit()
    Using.resource(blobService.ref2Stream(blobRef)) { is =>
      FileUtils.copyInputStreamToFile(is, tempFile)
      tempFile
    }
  end ref2TempFile

  /** Puts a zip file in the blobstore.
    */
  def putBlob(importZip: File): Validation[Throwable, BlobRef] =
    val provider = blobService.getDefaultProvider
    val blobName = createBlobName(importZip, "authoring/")
    blobService.putBlob(provider, blobName, UploadInfo.apply(importZip)).toValidation

  /** Creates a blob name out of an md5 hash of the file's stream.
    */
  def createBlobName(
    file: File,
    prefix: String
  ): String =
    Using.resource(new FileInputStream(file)) { is =>
      blobService.createBlobName(is, prefix)
    }
end File2BlobService
