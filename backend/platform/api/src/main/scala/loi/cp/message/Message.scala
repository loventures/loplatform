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

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{RequestBody, RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.de.web.Queryable.Trait
import com.learningobjects.de.web.{Queryable, QueryableId}
import loi.cp.attachment.AttachmentViewComponent

@Schema("message")
trait Message extends ComponentInterface with QueryableId:
  import Message.*

  @JsonProperty(LabelProperty)
  @Queryable
  def getLabel: MessageLabel

  @JsonProperty(ReadProperty)
  @Queryable
  def isRead: Boolean

  @JsonProperty(SubjectProperty)
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  def getSubject: String

  @JsonProperty
  def getBody: String

  @JsonProperty
  def getRecipients: Seq[Recipient]

//  @JsonProperty(DispositionProperty)
//  @Queryable
//  def getDisposition: MessageDisposition

  @JsonProperty(MessageIdProperty)
  @Queryable
  def getMessageId: Long

  @JsonProperty(InReplyToProperty)
  @Queryable
  def getInReplyTo: Option[Long]

  @JsonProperty(ThreadProperty)
  @Queryable
  def getThread: Long

  @JsonProperty(TimestampProperty)
  @Queryable
  def getTimestamp: Date

  @JsonProperty(SenderIdProperty)
  @Queryable
  def getSenderId: Long

  @JsonProperty(ContextIdProperty)
  @Queryable
  def getContextId: Option[Long]

  @RequestMapping(path = "attachment", method = Method.GET)
  def getAttachmentCount: Long

  @RequestMapping(path = "attachments", method = Method.Any)
  def getAttachments: AttachmentViewComponent

  @RequestMapping(path = "update", method = Method.POST)
  def update(@RequestBody flags: MessageFlags): Unit

  // internal
  def getStorageId: Long
end Message

object Message:
  final val LabelProperty     = "label"
  final val ReadProperty      = "read"
  final val SubjectProperty   = "subject"
//  final val DispositionProperty = "disposition"
  final val MessageIdProperty = "messageId"
  final val InReplyToProperty = "inReplyTo"
  final val ThreadProperty    = "thread"
  final val TimestampProperty = "timestamp"
  final val SenderIdProperty  = "sender_id"
  final val ContextIdProperty = "context_id"
end Message

case class MessageFlags(read: Option[Boolean], label: Option[MessageLabel])
