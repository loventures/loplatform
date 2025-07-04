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

package com.learningobjects.cpxp.component.chat

import java.util.Date

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.SqlIndex
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
@SqlIndex(value = "(room_id, timestamp)")
class ChatMessageFinder extends DomainEntity:

  @Column(columnDefinition = "TEXT")
  var body: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var room: ChatRoomFinder = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var sender: UserFinder = scala.compiletime.uninitialized

  @Column
  var timestamp: Date = scala.compiletime.uninitialized
end ChatMessageFinder

object ChatMessageFinder:
  final val ITEM_TYPE_CHAT_MESSAGE           = "ChatMessage"
  final val DATA_TYPE_CHAT_MESSAGE_SENDER    = "ChatMessage.sender"
  final val DATA_TYPE_CHAT_MESSAGE_TIMESTAMP = "ChatMessage.timestamp"
  final val DATA_TYPE_CHAT_MESSAGE_ROOM      = "ChatMessage.room"
  final val DATA_TYPE_CHAT_MESSAGE_BODY      = "ChatMessage.body"
