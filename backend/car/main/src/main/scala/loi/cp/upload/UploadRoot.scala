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

package loi.cp.upload

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ErrorResponse, FileResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.{UploadInfo, Uploader, Uploads}
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.attachment.Disposition
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.util.HttpUtils
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.apache.http.client.methods.HttpGet
import scaloi.syntax.any.*
import scaloi.syntax.option.*

import java.lang as jl
import scala.util.{Failure, Success, Try, Using}

@Component
class UploadRoot(
  val componentInstance: ComponentInstance,
  mws: MimeWebService
) extends UploadRootComponent
    with ComponentImplementation:
  private final val logger = org.log4s.getLogger

  override def upload(u: UploadInfo): Upload =
    // Attempt to determine mimetype from file suffix, and add a suffix if file doesn't have one.
    // Otherwise make the uploaded mimetype match mimetype from the suffix because that's what we
    // do on download anyways.
    Option(mws.getMimeType(u.getFileName)) match
      case None                  =>
        val fileExtension = Option(FilenameUtils.getExtension(u.getFileName))
        // If no extension, add one
        if fileExtension.isEmpty then
          Option(mws.getSuffix(u.getMimeType)) match
            case Some(suffix) => u.setFileName(s"${u.getFileName}.$suffix")
            case _            =>
      case Some(assumedMimeType) =>
        if !assumedMimeType.equals(u.getMimeType) then u.setMimeType(assumedMimeType)
    end match

    /* Bump the refcount of `u` to prevent deletion before the client can use it. */
    u.ref()

    uploadOf(u)
  end upload

  override def fetch(url: String): Upload =
    val filename = url.split("/").last
    val u        = UploadInfo.tempFile(filename)
    logger.info(s"Fetching: $url")
    val get      = new HttpGet(url) <| HttpUtils.enableRedirects
    Using.resource(HttpUtils.getHttpClient.execute(get)) { response =>
      logger.info(s"Status: ${response.getStatusLine}")
      if response.getStatusLine.getStatusCode != 200 then
        throw new Exception(s"Error downloading $url: ${response.getStatusLine.getStatusCode}")
      Using.resource(FileUtils.openOutputStream(u.getFile)) { out =>
        response.getEntity.writeTo(out)
      }
      val mimeType = Option.apply(mws.getMimeType(url)).getOrElse(response.getEntity.getContentType.getValue)
      u.setMimeType(mimeType)
    }
    Uploader.populateDimensions(u)
    uploadOf(u)
  end fetch

  private def uploadOf(u: UploadInfo): Upload =
    Upload(
      Uploads.createUpload(u),
      u.getFileName,
      u.getSize,
      u.getMimeType,
      Option(u.getWidth).map(_.intValue),
      Option(u.getHeight).map(_.intValue)
    )

  override def download(guid: String, download: jl.Boolean): WebResponse =
    Try(Uploads.retrieveUpload(guid)) match
      case Failure(e) =>
        ErrorResponse.notFound
      case Success(u) =>
        val mimeType = Option(mws.getMimeType(u.getFileName))
          .map(MediaType.parse)
          .getOrElse(MediaType.OCTET_STREAM)
        FileResponse(
          u.getFile,
          u.getFileName,
          mimeType,
          Current.getTime,
          if Option(download).isTrue then Disposition.attachment else Disposition.inline
        )

  override def delete(guid: String): Unit =
    // ignore not found
    Try(Uploads.consumeUpload(guid).destroy())
    ()
end UploadRoot
