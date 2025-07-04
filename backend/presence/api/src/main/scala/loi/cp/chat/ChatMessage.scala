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

package loi.cp.chat

import java.util.Date

import com.learningobjects.cpxp.service.presence.EventType

/** A chat event message.
  *
  * @param id
  *   the persistent message id
  * @param sender
  *   the sender of the message
  * @param room
  *   the chat room
  * @param timestamp
  *   the timestamp
  * @param message
  *   the message, maybe null if this is a typing signal
  * @param typing
  *   whether the user is currently typing
  */
case class ChatMessage(
  id: Option[Long],
  sender: String,
  room: Long,
  timestamp: Option[Date],
  message: Option[String],
  typing: Option[Boolean]
)

/** Chat message companion. */
object ChatMessage:

  /** Chat message event type evidence. */
  implicit val ChatMessageEventType: EventType[ChatMessage] = EventType("ChatMessage")
