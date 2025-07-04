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
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class AccessCodeBatchFinder extends PeerEntity:
  import AccessCodeBatchFinder.*

  @Column
  var createTime: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var creator: Item = scala.compiletime.uninitialized

  @Column
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var item: Item = scala.compiletime.uninitialized

  @Column
  var name: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_COMPONENT_IDENTIFIER)
  var componentId: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_JSON)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var json: JsonNode = scala.compiletime.uninitialized
end AccessCodeBatchFinder

object AccessCodeBatchFinder:
  final val ITEM_TYPE_ACCESS_CODE_BATCH             = "AccessCodeBatch"
  final val DATA_TYPE_JSON                          = "json"
  final val DATA_TYPE_ACCESS_CODE_BATCH_CREATOR     = "AccessCodeBatch.creator"
  final val DATA_TYPE_COMPONENT_IDENTIFIER          = "componentId"
  final val DATA_TYPE_ACCESS_CODE_BATCH_CREATE_TIME = "AccessCodeBatch.createTime"
  final val DATA_TYPE_ACCESS_CODE_BATCH_NAME        = "AccessCodeBatch.name"
  final val DATA_TYPE_ACCESS_CODE_BATCH_DISABLED    = "AccessCodeBatch.disabled"
  final val DATA_TYPE_ACCESS_CODE_BATCH_ITEM        = "AccessCodeBatch.item"
