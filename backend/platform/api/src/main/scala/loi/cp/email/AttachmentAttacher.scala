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

package loi.cp.email

import javax.mail.Part
import javax.mail.internet.MimeBodyPart

import com.learningobjects.cpxp.service.attachment.{AttachmentFacade, AttachmentWebService}
import scaloi.syntax.AnyOps.*
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.util.TempFileMap
import org.apache.commons.io.FileUtils
import scala.util.Using

/** Attaches database attachments to MIME messages.
  * @param aws
  *   the attachment web service
  * @param mws
  *   the mime web service
  */
class AttachmentAttacher(implicit aws: AttachmentWebService, mws: MimeWebService) extends CanAttach[AttachmentFacade]:
  val files = new TempFileMap("email", ".att")

  /** Convert a database attachment to a MIME body part.
    * @param attachment
    *   the thing to attach
    * @return
    *   the mime body part
    */
  override def attachmentPart(attachment: AttachmentFacade): MimeBodyPart =
    new MimeBodyPart() <| { part =>
      val tmpFile = files.create(attachment.getId.toString)
      Using.resource(aws.getAttachmentBlob(attachment.getId).openInputStream) { in =>
        FileUtils.copyInputStreamToFile(in, tmpFile)
      }
      part.attachFile(tmpFile)
      part.setDisposition(Part.ATTACHMENT)
      part.setFileName(attachment.getFileName)
      part.setHeader("Content-Type", mws.getMimeType(attachment.getFileName))
    }
end AttachmentAttacher
