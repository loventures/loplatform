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

package com.learningobjects.cpxp.service.email

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.component.ComponentConstants
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date
import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
@SqlIndex("(id) WHERE success IS NULL AND del IS NULL") // index for poller
class EmailFinder extends LeafEntity:

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var body: JsonNode = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(function = IndexType.NORMAL, byParent = true, nonDeleted = true)
  var entity: jl.Long = scala.compiletime.uninitialized

  @Column
  var noReply: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var sent: Date = scala.compiletime.uninitialized

  @Column
  var success: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER)
  var componentId: String = scala.compiletime.uninitialized
end EmailFinder

object EmailFinder:
  final val ITEM_TYPE_EMAIL          = "Email"
  final val DATA_TYPE_EMAIL_NO_REPLY = "Email.noReply"
  final val DATA_TYPE_EMAIL_SENT     = "Email.sent"
  final val DATA_TYPE_EMAIL_BODY     = "Email.body"
  final val DATA_TYPE_EMAIL_ENTITY   = "Email.entity"
  final val DATA_TYPE_EMAIL_SUCCESS  = "Email.success"
