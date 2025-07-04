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

package loi.cp.reply

import java.util.Date

import javax.mail.internet.MimeMessage
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.WebResponse
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.attachment.AttachmentComponent
import loi.cp.email.{MarshalEmailSupport, UnmarshalEmailSupport}
import scala.util.Using

import scala.util.Try

@Component
class ReplyReceiptImpl(
  val componentInstance: ComponentInstance,
  self: ReplyFacade,
  aws: AttachmentWebService,
)(implicit cs: ComponentService, fs: FacadeService)
    extends ReplyReceipt
    with ComponentImplementation:
  override def getId = componentInstance.getId

  override def getSender: Option[String] = self.getSender

  override def getDate: Option[Date] = self.getDate

  override def getMessageId: Option[String] = self.getMessageId

  override def getEntity: Option[Long] = self.getEntity

  override def getSubject: String = decodedEmail.subject

  override def getBody: String = decodedEmail.body

  override def download: WebResponse =
    self.getAttachment.component[AttachmentComponent].view(true, false, null)

  private lazy val decodedEmail: DecodedEmail = Try {
    Using.resource(openAttachment) { in =>
      val message = new MimeMessage(MarshalEmailSupport.mailSession, in)
      DecodedEmail(message.getSubject, UnmarshalEmailSupport.plaintextContent(message))
    }
  } getOrElse DecodedEmail("<error>", "")

  private def openAttachment =
    aws.getAttachmentBlob(self.getAttachment.getId).openInputStream
end ReplyReceiptImpl

case class DecodedEmail(subject: String, body: String)
