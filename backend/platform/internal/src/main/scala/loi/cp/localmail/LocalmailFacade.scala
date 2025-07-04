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

import java.util.Date

import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.{Facade, FacadeChild, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.attachment.AttachmentFacade
import com.learningobjects.cpxp.service.localmail.LocalmailFinder

@FacadeItem(LocalmailFinder.ITEM_TYPE_LOCALMAIL)
trait LocalmailFacade extends Facade:
  @FacadeData
  def getToAddress: String
  def setToAddress(address: String): Unit

  @FacadeData
  def getToName: String
  def setToName(name: String): Unit

  @FacadeData
  def getFromAddress: String
  def setFromAddress(address: String): Unit

  @FacadeData
  def getFromName: String
  def setFromName(name: String): Unit

  @FacadeData
  def getMessageId: String
  def setMessageId(messageId: String): Unit

  @FacadeData
  def getInReplyTo: String
  def setInReplyTo(inReplyTo: String): Unit

  @FacadeData
  def getDate: Date
  def setDate(date: Date): Unit

  @FacadeData
  def getSubject: String
  def setSubject(subject: String): Unit

  @FacadeData
  def getBody: String
  def setBody(body: String): Unit

  @FacadeChild
  def addAttachment(upload: UploadInfo): AttachmentFacade
  def getAttachments: List[AttachmentFacade]
end LocalmailFacade
