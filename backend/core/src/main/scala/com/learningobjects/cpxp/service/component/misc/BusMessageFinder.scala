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
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import jakarta.persistence.*
import org.hibernate.annotations.{JdbcType, Type}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl
import java.util.Date

@Entity
@SqlIndex("(bus_id, scheduled) WHERE (state = 'Ready' OR state = 'Queued')")
class BusMessageFinder extends DomainEntity:
  import BusMessageFinder.*

  @Column
  var attempts: jl.Long = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var body: JsonNode = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var bus: MessageBusFinder = scala.compiletime.uninitialized

  @Column
  var scheduled: Date = scala.compiletime.uninitialized

  @Column(length = 32)
  var state: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_BUS_MESSAGE_TYPE)
  var busMessageType: String = scala.compiletime.uninitialized
end BusMessageFinder

object BusMessageFinder:
  final val ITEM_TYPE_BUS_MESSAGE           = "BusMessage"
  final val DATA_TYPE_BUS_MESSAGE_SCHEDULED = "BusMessage.scheduled"
  final val DATA_TYPE_BUS_MESSAGE_BUS       = "BusMessage.bus"
  final val DATA_TYPE_BUS_MESSAGE_BODY      = "BusMessage.body"
  final val DATA_TYPE_BUS_MESSAGE_STATE     = "BusMessage.state"
  final val DATA_TYPE_BUS_MESSAGE_TYPE      = "BusMessage.type"
  final val DATA_TYPE_BUS_MESSAGE_ATTEMPTS  = "BusMessage.attempts"
