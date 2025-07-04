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

package loi.cp.redirect

import com.github.tototoshi.csv.CSVReader
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.controller.upload.{UploadInfo, Uploads}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.util.BlobInfo
import loi.cp.attachment.AttachmentComponent
import org.apache.commons.codec.CharEncoding

import java.io.InputStreamReader
import java.lang as jl
import scala.util.Using

@Component
class RedirectImpl(val componentInstance: ComponentInstance, self: RedirectFacade)(implicit
  aws: AttachmentWebService,
  fs: FacadeService,
  mws: MimeWebService,
  cs: ComponentService
) extends Redirect
    with ComponentImplementation:

  @PostCreate
  override def update(init: Redirect): Redirect =
    self.pollute()
    self.setName(init.getName)
    self.setDisabled(init.isDisabled)
    Option(Uploads.consumeUpload(init.getCsvUpload)) foreach { upload =>
      if upload ne UploadInfo.REMOVE then
        if !MediaType.parse(mws.getMimeType(upload.getFileName)).is(MediaType.CSV_UTF_8.withoutParameters) then
          throw new ValidationException("csv", upload.getFileName, "Not a CSV") // I bring shame upon us
      self.setActiveAttachment(upload)
    }
    this
  end update

  override def getId: jl.Long = self.getId

  override def getName: String = self.getName

  override def isDisabled: Boolean = self.getDisabled

  override def getCsv: AttachmentComponent = self.getActiveAttachment.component[AttachmentComponent]

  override def getCsvUpload: String = ??? // write-only

  override def delete(): Unit = self.delete()

  override def getRedirects: Map[String, String] = parseRedirects.toMap

  private def parseRedirects: Seq[(String, String)] =
    Using.resource(CSVReader.open(new InputStreamReader(blob.openInputStream, CharEncoding.UTF_8))) { csv =>
      csv.all() collect {
        case key :: value :: tail if !key.isEmpty => key -> value
      }
    }

  private def blob: BlobInfo = aws.getAttachmentBlob(self.getActiveAttachment.getId)
end RedirectImpl
