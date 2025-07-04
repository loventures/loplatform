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

package com.learningobjects.cpxp.service.notification

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
import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
@SqlIndex("(id) WHERE del IS NULL AND processed IS NULL") // date index instead?
class NotificationFinder extends LeafEntity:
  import NotificationFinder.*

  @ManyToOne(fetch = FetchType.LAZY)
  var context: GroupFinder = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_NOTIFICATION_DATA)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var notificationData: JsonNode = scala.compiletime.uninitialized

  @Column
  var processed: jl.Boolean = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var sender: UserFinder = scala.compiletime.uninitialized

  @Column
  var time: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_COMPONENT_IDENTIFIER)
  var componentId: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_NOTIFICATION_TOPIC)
  var topic: String = scala.compiletime.uninitialized

  /** deprecated */
  @Column
  @DataType(DATA_TYPE_NOTIFICATION_TOPIC_ID)
  var topic_id: jl.Long = scala.compiletime.uninitialized
end NotificationFinder

object NotificationFinder:
  final val ITEM_TYPE_NOTIFICATION           = "Notification"
  final val DATA_TYPE_COMPONENT_IDENTIFIER   = "componentId"
  final val DATA_TYPE_NOTIFICATION_PROCESSED = "Notification.processed"
  final val DATA_TYPE_NOTIFICATION_TIME      = "Notification.time"
  final val DATA_TYPE_NOTIFICATION_CONTEXT   = "Notification.context"
  final val DATA_TYPE_NOTIFICATION_SENDER    = "Notification.sender"
  final val DATA_TYPE_NOTIFICATION_DATA      = "Notification.data"
  final val DATA_TYPE_NOTIFICATION_TOPIC     = "Notification.topic"

  /** deprecated */
  final val DATA_TYPE_NOTIFICATION_TOPIC_ID = "Notification.topic_id"
end NotificationFinder
