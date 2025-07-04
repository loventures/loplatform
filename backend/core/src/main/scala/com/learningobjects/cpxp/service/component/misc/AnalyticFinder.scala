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
import jakarta.persistence.*
import org.hibernate.annotations.{JdbcType, Type}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date

@Entity
class AnalyticFinder extends DomainEntity:
  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var datajson: JsonNode = scala.compiletime.uninitialized

  @Column
  var guid: String = scala.compiletime.uninitialized

  @Column
  var time: Date = scala.compiletime.uninitialized
end AnalyticFinder

object AnalyticFinder:
  final val ITEM_TYPE_DEAN_ANALYTIC  = "Analytic"
  final val DATA_TYPE_DEAN_TIME      = "Analytic.time"
  final val DATA_TYPE_DEAN_GUID      = "Analytic.guid"
  final val DATA_TYPE_DEAN_DATA_JSON = "Analytic.datajson"
