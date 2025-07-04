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

package com.learningobjects.cpxp.service.user

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date
import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class UserHistoryFinder extends LeafEntity:
  import UserHistoryFinder.*

  @Column
  var accessTime: Date = scala.compiletime.uninitialized

  @Column
  var authTime: Date = scala.compiletime.uninitialized

  @Column
  var loginCount: jl.Long = scala.compiletime.uninitialized

  @Column
  var loginTime: Date = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_JSON)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var json: JsonNode = scala.compiletime.uninitialized
end UserHistoryFinder

object UserHistoryFinder:
  final val ITEM_TYPE_USER_HISTORY    = "UserHistory"
  final val DATA_TYPE_JSON            = "json"
  final val DATA_TYPE_LOGIN_TIME      = "UserHistory.loginTime"
  final val DATA_TYPE_LOGIN_AUTH_TIME = "UserHistory.authTime"
  final val DATA_TYPE_ACCESS_TIME     = "UserHistory.accessTime"
  final val DATA_TYPE_LOGIN_COUNT     = "UserHistory.loginCount"
