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

package com.learningobjects.cpxp.service.internationalization

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
class InternationalizationFinder extends PeerEntity:
  import InternationalizationFinder.*

  @Column
  var internal: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var locale: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var messages: JsonNode = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DISABLED)
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_NAME)
  var name: String = scala.compiletime.uninitialized
end InternationalizationFinder

object InternationalizationFinder:
  final val ITEM_TYPE_INTERNATIONALIZATION          = "Internationalization"
  final val DATA_TYPE_NAME                          = "name"
  final val DATA_TYPE_INTERNATIONALIZATION_LOCALE   = "Internationalization.locale"
  final val DATA_TYPE_INTERNATIONALIZATION_MESSAGES = "Internationalization.messages"
  final val DATA_TYPE_DISABLED                      = "disabled"
  final val DATA_TYPE_INTERNATIONALIZATION_INTERNAL = "Internationalization.internal"
