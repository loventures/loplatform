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

package com.learningobjects.cpxp.service.component.misc

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.integration.SystemFinder
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class MessageBusFinder extends PeerEntity:

  @Column
  var scheduled: Date = scala.compiletime.uninitialized

  @Column(length = 32)
  var state: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var statistics: JsonNode = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  @FunctionalIndex(byParent = true, nonDeleted = true, function = IndexType.NORMAL)
  var system: SystemFinder = scala.compiletime.uninitialized
end MessageBusFinder

object MessageBusFinder:
  final val ITEM_TYPE_MESSAGE_BUS            = "MessageBus"
  final val DATA_TYPE_MESSAGE_BUS_STATISTICS = "MessageBus.statistics"
  final val DATA_TYPE_MESSAGE_BUS_SCHEDULED  = "MessageBus.scheduled"
  final val DATA_TYPE_MESSAGE_BUS_STATE      = "MessageBus.state"
  final val DATA_TYPE_MESSAGE_BUS_SYSTEM     = "MessageBus.system"
