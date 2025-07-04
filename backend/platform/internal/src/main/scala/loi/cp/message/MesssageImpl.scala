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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.attachment.AttachmentViewComponent

@Component
class MesssageImpl(
  val componentInstance: ComponentInstance,
  self: MessageFacade
)(implicit cs: ComponentService, fs: FacadeService)
    extends Message
    with ComponentImplementation:
  private val storage: MessageStorageFacade = self.getStorage

  override def getId = getComponentInstance.getId

  override def getLabel: MessageLabel = self.getLabel

  override def isRead: Boolean = self.getRead

  override def getMessageId: Long = storage.getId.longValue

  override def getInReplyTo: Option[Long] = storage.getInReplyTo

  override def getSubject: String = storage.getSubject

  override def getBody: String = storage.getBody

  override def getRecipients: Seq[Recipient] = storage.getRecipients

  override def getTimestamp: Date = storage.getTimestamp

  override def getThread: Long = storage.getThread

  override def getSenderId: Long = storage.getSender

  override def getContextId: Option[Long] = storage.getContext

  override def getStorageId: Long = storage.getId

  override def getAttachmentCount: Long =
    getAttachments.getAttachments(ApiQuery.COUNT).getTotalCount.longValue

  override def getAttachments: AttachmentViewComponent =
    storage.component[AttachmentViewComponent]

  override def update(flags: MessageFlags): Unit =
    flags.read foreach { read =>
      self.setRead(read)
    }
    flags.label foreach { label =>
      self.setLabel(label)
    }
end MesssageImpl
