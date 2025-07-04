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

package com.learningobjects.cpxp.service.notification

import argonaut.Json
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import com.learningobjects.cpxp.postgresql.ArgonautUserType
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class SubscribeFinder extends LeafEntity:

  @ManyToOne(fetch = FetchType.LAZY)
  @FunctionalIndex(byParent = false, nonDeleted = true, function = IndexType.NORMAL)
  var context: Item = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[ArgonautUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var subscriptions: Json = scala.compiletime.uninitialized

  /** deprecated */
  @Column
  var interest: jl.Long = scala.compiletime.uninitialized

  /** deprecated */
  @ManyToOne(fetch = FetchType.LAZY)
  var item: Item = scala.compiletime.uninitialized
end SubscribeFinder

object SubscribeFinder:
  final val ITEM_TYPE_SUBSCRIBE               = "Subscribe"
  final val DATA_TYPE_SUBSCRIBE_CONTEXT       = "Subscribe.context"
  final val DATA_TYPE_SUBSCRIBE_SUBSCRIPTIONS = "Subscribe.subscriptions"

  /** deprecated */
  final val DATA_TYPE_SUBSCRIBE_ITEM = "Subscribe.item"

  /** deprecated */
  final val DATA_TYPE_SUBSCRIBE_INTEREST = "Subscribe.interest"
end SubscribeFinder
