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

package com.learningobjects.cpxp.service.token

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class TokenFinder extends LeafEntity:
  import TokenFinder.*

  @Column
  var accepted: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var expires: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_TOKEN_ID)
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var tid: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_TOKEN_TYPE)
  @Enumerated(EnumType.STRING)
  var ttype: TokenType = scala.compiletime.uninitialized
end TokenFinder

object TokenFinder:
  final val ITEM_TYPE_TOKEN            = "Token"
  final val DATA_TYPE_TOKEN_EXPIRES    = "Token.expires"
  final val DATA_TYPE_TOKEN_ACCEPTANCE = "Token.accepted"
  final val DATA_TYPE_TOKEN_TYPE       = "Token.type"
  final val DATA_TYPE_TOKEN_ID         = "Token.id"
