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

import java.util.Properties
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import javax.mail.{Part, Session}

import com.learningobjects.cpxp.controller.upload.UploadInfo
import scaloi.syntax.AnyOps.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.util.{DeXssSupport, MimeUtils}

import scala.util.Try

/** Support for marshalling an email message.
  */
object MarshalEmailSupport:

  /** Get a default mail session. TODO: KILL ME AND DI THIS. */
  lazy val mailSession: Session = Session.getInstance(new Properties())

  /** No reply email prefix. */
  val NoReply = "noreply"

  /** Construct a MIME message.
    * @param messageId
    *   the message id
    * @return
    *   the MIME message
    */
  def mimeMessage(messageId: String): MimeMessage =
    new MimeMessage(mailSession):
      override def updateMessageID(): Unit =
        setHeader(MessageHeaders.MessageID, messageId)

  /** Construct a MIME email part.
    * @param body
    *   the text content of the email
    * @param asHtml
    *   whether the content is HTML
    * @return
    *   a MIME email part
    */
  def contentPart(body: String, asHtml: Boolean): MimeMultipart =
    contentPart(body, asHtml = asHtml, List.empty[UploadInfo]*)

  /** Construct a MIME email part.
    * @param body
    *   the text content of the email
    * @param asHtml
    *   whether the content is HTML
    * @param attachments
    *   any attachments to include in the email
    * @tparam A
    *   the attachment representation type
    * @return
    *   a MIME email part
    */
  def contentPart[A: CanAttach](body: String, asHtml: Boolean, attachments: A*): MimeMultipart =
    new MimeMultipart("related") <| { content =>
      content `addBodyPart` bodyPart(body, asHtml)
      attachments foreach { upload =>
        content `addBodyPart` implicitly[CanAttach[A]].attachmentPart(upload)
      }
    }

  /** Construct a MIME body part.
    * @param body
    *   the body text
    * @param asHtml
    *   whether the content is HTML
    * @return
    *   the body part
    */
  def bodyPart(body: String, asHtml: Boolean): MimeBodyPart =
    new MimeBodyPart() <| { part =>
      if asHtml then
        val sanitized = DeXssSupport.deXss(body, "email_support")
        part.setContent(sanitized, MimeUtils.MIME_TYPE_TEXT_HTML + MimeUtils.CHARSET_SUFFIX_UTF_8)
      else
        part.setText(body)
        part.setHeader("Content-Type", MimeUtils.MIME_TYPE_TEXT_PLAIN + MimeUtils.CHARSET_SUFFIX_UTF_8)
    }

  /** Try to parse an email address.
    * @param email
    *   the textual email address
    * @return
    *   the internet address
    */
  def parseInternetAddress(email: String): Try[InternetAddress] =
    Try(new InternetAddress(email))

  /** No reply at this domain.
    * @return
    *   no reply at this domain
    */
  def noreplyAtDomain: InternetAddress =
    new InternetAddress(s"$NoReply@${Current.getDomainDTO.hostName}", Current.getDomainDTO.name)

  /** No reply at this domain with a specified personal name.
    * @param name
    *   the personal name
    * @return
    *   no reply at this domain
    */
  def noreplyAtDomain(name: String): InternetAddress =
    new InternetAddress(s"$NoReply@${Current.getDomainDTO.hostName}", name)

  /** Construct the reply address for an email from this system.
    * @param name
    *   the personal name of the sender of this email
    * @param recipient
    *   the identifier of the recipient of this email
    * @param id
    *   the identifier of the email entity that generated this
    * @param replyDomain
    *   the domain to use for generating the reply address
    * @return
    *   a reply address
    */
  def replyAddress(name: String, recipient: Long, id: Long, replyDomain: Option[String]): InternetAddress =
    replyDomain.fold(noreplyAtDomain(personalName(name))) { domain =>
      val address =
        EmailAddress(EmailKeys.obfuscate(recipient, id), domain).toAddress
      new InternetAddress(address, personalName(name))
    }

  /** Construct an internet address.
    * @param name
    *   the personal name
    * @param address
    *   the email address
    * @return
    *   the internet address
    */
  def toAddress(name: String, address: String): InternetAddress =
    new InternetAddress(address, personalName(name))

  /** Construct the message id for an email from this domain.
    * @param id
    *   the email id
    * @return
    *   the message identifier
    */
  def domainMessageId(id: Long): String =
    domainMessageId(EmailKeys.obfuscate(id, Current.getDomain))

  /** Construct a message identifier for this domain.
    * @param name
    *   the message id name part
    * @return
    *   the message identifier
    */
  private def domainMessageId(name: String): String =
    MessageIdentifier(name, Current.getDomainDTO.hostName).toMessageId

  /** Construct the personal name for an email from this domain. Appends the domain short name to the specified personal
    * name.
    * @param name
    *   the name part
    * @return
    *   the personal name
    */
  private def personalName(name: String): String =
    s"$name (${Current.getDomainDTO.shortName})"

  /** An upload attacher. Converts uploads into mime body part attachments.
    */
  implicit val uploadAttacher: CanAttach[UploadInfo] = (upload: UploadInfo) =>
    new MimeBodyPart() <| { part =>
      part.attachFile(upload.getFile)
      part.setDisposition(Part.ATTACHMENT)
      part.setFileName(upload.getFileName)
      part.setHeader("Content-Type", upload.getMimeType)
    }
end MarshalEmailSupport
