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

package com.learningobjects.cpxp.component.messaging

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class MessageStorageFinder extends PeerEntity:
  import MessageStorageFinder.*

  @Column(columnDefinition = "TEXT")
  @DataType(DATA_TYPE_MESSAGE_BODY)
  var body: String = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_MESSAGE_CONTEXT)
  @ManyToOne(fetch = FetchType.LAZY)
  var context: GroupFinder = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_MESSAGE_IN_REPLY_TO)
  @ManyToOne(fetch = FetchType.LAZY)
  var inReplyTo: MessageStorageFinder = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_MESSAGE_METADATA)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var metadata: JsonNode = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_MESSAGE_SENDER)
  @ManyToOne(fetch = FetchType.LAZY)
  var sender: UserFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_MESSAGE_SUBJECT)
  var subject: String = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_MESSAGE_THREAD)
  @ManyToOne(fetch = FetchType.LAZY)
  var thread: MessageStorageFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_MESSAGE_TIMESTAMP)
  var timestamp: Date = scala.compiletime.uninitialized
end MessageStorageFinder

object MessageStorageFinder:
  final val ITEM_TYPE_MESSAGE_STORAGE     = "MessageStorage"
  final val DATA_TYPE_MESSAGE_CONTEXT     = "Message.context"
  final val DATA_TYPE_MESSAGE_IN_REPLY_TO = "Message.inReplyTo"
  final val DATA_TYPE_MESSAGE_METADATA    = "Message.metadata"
  final val DATA_TYPE_MESSAGE_SENDER      = "Message.sender"
  final val DATA_TYPE_MESSAGE_TIMESTAMP   = "Message.timestamp"
  final val DATA_TYPE_MESSAGE_THREAD      = "Message.thread"
  final val DATA_TYPE_MESSAGE_BODY        = "Message.body"
  final val DATA_TYPE_MESSAGE_SUBJECT     = "Message.subject"
end MessageStorageFinder
