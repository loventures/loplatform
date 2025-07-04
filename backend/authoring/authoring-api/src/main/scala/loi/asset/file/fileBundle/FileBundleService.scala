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

package loi.asset.file.fileBundle

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.util.{DateUtils, LocalFileInfo}
import loi.authoring.asset.Asset
import loi.authoring.blob.BlobService
import loi.authoring.configuration.AuthoringConfigurationService

import java.io.File
import java.util.Date
import scala.util.Try

@Service
class FileBundleService(
  attachmentWebService: AttachmentWebService,
  blobService: BlobService,
  configService: AuthoringConfigurationService,
  mimeWebService: MimeWebService
):

  /** Gets the file at `pathInZip` from `bundle`. Fails in many ways, possibly:
    *   - NoSuchBlobRef
    *   - AssetHasNoAttachment
    *   - IOException (no FileCache dir, failure to create unpacking dir)
    *
    * Note that the File might not exist, use `.exists` to check
    *
    * @param bundle
    *   the file bundle
    * @param pathInZip
    *   path to file, do not include leading slash
    * @return
    *   the file or null if it doesn't exist
    */
  def fileInfo(bundle: Asset[?], pathInZip: String): Try[LocalFileInfo] =
    lazy val contentType = mimeWebService.getMimeType(pathInZip)
    blobService
      .requireBlobInfo(bundle)
      .map(blobInfo =>
        val file = attachmentWebService.getZipBlobFile(blobInfo, pathInZip)
        toFileInfo(file, blobInfo.getLastModified, contentType, blobInfo.getBlobName)
      )

  private def toFileInfo(
    file: File,
    lastModified: Date,
    contentType: String,
    invalidationKeySuffix: String
  ): LocalFileInfo =
    val fi = new LocalFileInfo(file)
    fi.setLastModified(lastModified)
    fi.setContentType(contentType)
    fi.setDoCache(true)
    fi.setExpires(DateUtils.Unit.day.getValue)
    fi
  end toFileInfo
end FileBundleService
