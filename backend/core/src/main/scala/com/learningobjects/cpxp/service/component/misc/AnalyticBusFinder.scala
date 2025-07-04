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
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.integration.SystemFinder
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class AnalyticBusFinder extends PeerEntity:

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var configuration: JsonNode = scala.compiletime.uninitialized

  @Column
  var failureCount: jl.Long = scala.compiletime.uninitialized

  @Column
  var scheduled: Date = scala.compiletime.uninitialized

  @Column(length = 32)
  var state: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var statistics: JsonNode = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var system: SystemFinder = scala.compiletime.uninitialized

  @Column
  var senderIdentifier: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var windowIds: JsonNode = scala.compiletime.uninitialized

  @Column
  var windowTime: jl.Long = scala.compiletime.uninitialized

  @Column
  var lastMaterializedViewRefreshDate: Date = scala.compiletime.uninitialized
end AnalyticBusFinder

object AnalyticBusFinder:
  final val ITEM_TYPE_ANALYTIC_BUS                   = "AnalyticBus"
  final val DATA_TYPE_ANALYTIC_BUS_STATISTICS        = "AnalyticBus.statistics"
  final val DATA_TYPE_ANALYTIC_BUS_WINDOW_IDS        = "AnalyticBus.windowIds"
  final val DATA_TYPE_ANALYTIC_BUS_CONFIGURATION     = "AnalyticBus.configuration"
  final val DATA_TYPE_ANALYTIC_BUS_SCHEDULED         = "AnalyticBus.scheduled"
  final val DATA_TYPE_ANALYTIC_BUS_SYSTEM            = "AnalyticBus.system"
  final val DATA_TYPE_ANALYTIC_BUS_SENDER_IDENTIFIER = "AnalyticBus.senderIdentifier";
  final val DATA_TYPE_ANALYTIC_BUS_WINDOW_TIME       = "AnalyticBus.windowTime"
  final val DATA_TYPE_ANALYTIC_BUS_FAILURE_COUNT     = "AnalyticBus.failureCount"
  final val DATA_TYPE_ANALYTIC_BUS_STATE             = "AnalyticBus.state"
  final val DATA_TYPE_ANALYTIC_BUS_LAST_MVR_DATE     = "AnalyticBus.lastMaterializedViewRefreshDate"
end AnalyticBusFinder
