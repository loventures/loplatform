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

package loi.cp.localmail

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.email.LocalmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import javax.mail.internet.{InternetAddress, MimeMessage}
import loi.cp.email.{MessageHeaders, UnmarshalEmailSupport}
import scaloi.misc.TimeSource

@Service
class LocalmailServiceImpl(
  now: TimeSource,
)(implicit
  fs: FacadeService
) extends LocalmailService:
  import UnmarshalEmailSupport.*

  override def sendLocalmail(to: InternetAddress, message: MimeMessage): Unit =
    val newLocalmail          = LocalmailWebControllerImpl.folder.addLocalmail()
    newLocalmail.setToName(to.getPersonal)
    newLocalmail.setToAddress(to.getAddress)
    val from: InternetAddress = message.getFrom.head.asInstanceOf[InternetAddress]
    newLocalmail.setFromAddress(from.getAddress)
    newLocalmail.setFromName(from.getPersonal)
    newLocalmail.setDate(now.date)
    newLocalmail.setMessageId(message.getMessageID)
    newLocalmail.setInReplyTo(message.headerOpt(MessageHeaders.InReplyTo).orNull)
    newLocalmail.setSubject(message.getSubject)
    newLocalmail.setBody(htmlContent(message))
    attachmentFiles(message) foreach newLocalmail.addAttachment
  end sendLocalmail

  override def htmlContent(message: MimeMessage): String =
    UnmarshalEmailSupport.htmlContent(message)
end LocalmailServiceImpl
