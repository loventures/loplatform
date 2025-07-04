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

package loi.cp.message

import java.util.Date

import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.attachment.{AttachmentConstants, AttachmentFacade}
import com.learningobjects.cpxp.component.messaging.MessageConstants.*

@FacadeItem(ITEM_TYPE_MESSAGE_STORAGE)
trait MessageStorageFacade extends Facade:
  @FacadeData(DATA_TYPE_MESSAGE_SUBJECT)
  def getSubject: String
  def setSubject(subject: String): Unit

  @FacadeData(DATA_TYPE_MESSAGE_BODY)
  def getBody: String
  def setBody(body: String): Unit

  @FacadeData(DATA_TYPE_MESSAGE_TIMESTAMP)
  def getTimestamp: Date
  def setTimestamp(timestamp: Date): Unit

  @FacadeData(DATA_TYPE_MESSAGE_IN_REPLY_TO)
  def getInReplyTo: Option[Long]
  def setInReplyTo(irt: Option[Long]): Unit

  @FacadeData(DATA_TYPE_MESSAGE_THREAD)
  def getThread: Long
  def setThread(thread: Long): Unit

  @FacadeData(DATA_TYPE_MESSAGE_SENDER)
  def getSender: Long
  def setSender(sender: Long): Unit

  @FacadeData(DATA_TYPE_MESSAGE_CONTEXT)
  def getContext: Option[Long]
  def setContext(context: Option[Long]): Unit

  @FacadeJson(DATA_TYPE_MESSAGE_METADATA)
  def getRecipients: Seq[Recipient]
  def setRecipients(recipients: Seq[Recipient]): Unit

  @FacadeChild(AttachmentConstants.ITEM_TYPE_ATTACHMENT)
  def addAttachment(upload: UploadInfo): AttachmentFacade
  def getAttachments: java.util.List[AttachmentFacade]
end MessageStorageFacade
