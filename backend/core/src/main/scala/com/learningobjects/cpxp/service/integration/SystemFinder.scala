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

package com.learningobjects.cpxp.service.integration

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class SystemFinder extends PeerEntity:
  import SystemFinder.*

  @Column
  var allowLogin: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var callbackPath: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_SYSTEM_ID)
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var systemId: String = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var key: String = scala.compiletime.uninitialized

  @Column
  var lmsUrl: String = scala.compiletime.uninitialized

  @Column
  @FriendlyName
  var name: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  @DataType(DATA_TYPE_COMPONENT_CONFIGURATION)
  var componentConfiguration: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_COMPONENT_IDENTIFIER)
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var componentId: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DISABLED)
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_JSON)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var json: JsonNode = scala.compiletime.uninitialized
end SystemFinder

object SystemFinder:
  final val ITEM_TYPE_SYSTEM                  = "System"
  final val DATA_TYPE_SYSTEM_ID               = "System.id"
  final val DATA_TYPE_SYSTEM_URL              = "System.lmsUrl"
  final val DATA_TYPE_JSON                    = "json"
  final val DATA_TYPE_COMPONENT_IDENTIFIER    = "componentId"
  final val DATA_TYPE_CALLBACK_PATH           = "System.callbackPath"
  final val DATA_TYPE_SYSTEM_ALLOW_LOGIN      = "System.allowLogin"
  final val DATA_TYPE_DISABLED                = "disabled"
  final val DATA_TYPE_COMPONENT_CONFIGURATION = "componentConfiguration"
  final val DATA_TYPE_SYSTEM_NAME             = "System.name"
  final val DATA_TYPE_SYSTEM_KEY              = "System.key"
end SystemFinder
