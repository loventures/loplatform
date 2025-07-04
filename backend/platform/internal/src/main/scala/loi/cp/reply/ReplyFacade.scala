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

import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.{Facade, FacadeChild, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.attachment.{AttachmentConstants, AttachmentFacade}
import com.learningobjects.cpxp.service.reply.ReplyFinder.*

/** Reply facade. This represents a reply that the system received to an email that the system sent out.
  */
@FacadeItem(ITEM_TYPE_REPLY)
trait ReplyFacade extends Facade:
  @FacadeData(value = DATA_TYPE_REPLY_SENDER)
  def getSender: Option[String]
  def setSender(sender: Option[String]): Unit

  @FacadeData(value = DATA_TYPE_REPLY_DATE)
  def getDate: Option[Date]
  def setDate(date: Option[Date]): Unit

  @FacadeData(value = DATA_TYPE_REPLY_MESSAGE_ID)
  def getMessageId: Option[String]
  def setMessageId(messageId: Option[String]): Unit

  @FacadeData(value = DATA_TYPE_REPLY_ENTITY)
  def getEntity: Option[Long]
  def setEntity(entity: Option[Long]): Unit

  @FacadeChild(AttachmentConstants.ITEM_TYPE_ATTACHMENT)
  def getAttachment: AttachmentFacade
  def addAttachment(ui: UploadInfo): AttachmentFacade
end ReplyFacade
