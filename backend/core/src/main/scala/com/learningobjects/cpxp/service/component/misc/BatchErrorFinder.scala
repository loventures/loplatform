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
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class BatchErrorFinder extends PeerEntity:

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var error: JsonNode = scala.compiletime.uninitialized

  @Column
  var index: jl.Long = scala.compiletime.uninitialized

  @Column
  var reason: String = scala.compiletime.uninitialized
end BatchErrorFinder

object BatchErrorFinder:
  final val ITEM_TYPE_BATCH_ERROR        = "BatchError"
  final val DATA_TYPE_BATCH_ERROR_INDEX  = "BatchError.index"
  final val DATA_TYPE_BATCH_ERROR_REASON = "BatchError.reason"
  final val DATA_TYPE_BATCH_ERROR_ERROR  = "BatchError.error"
