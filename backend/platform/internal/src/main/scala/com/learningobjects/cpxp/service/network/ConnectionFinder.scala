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

package com.learningobjects.cpxp.service.network

import java.util.Date
import java.lang as jl
import jakarta.persistence.*

import com.learningobjects.cpxp.entity.LeafEntity
import com.learningobjects.cpxp.service.item.Item
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class ConnectionFinder extends LeafEntity:
  @ManyToOne(fetch = FetchType.LAZY)
  var network: NetworkFinder = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var user: Item = scala.compiletime.uninitialized // TODO: this should be UserFinder

  @Column
  var active: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var created: Date = scala.compiletime.uninitialized
end ConnectionFinder

object ConnectionFinder:
  final val ITEM_TYPE_CONNECTION = "Connection"

  final val DATA_TYPE_CONNECTION_NETWORK = "Connection.network"

  final val DATA_TYPE_CONNECTION_USER = "Connection.user"

  final val DATA_TYPE_CONNECTION_ACTIVE = "Connection.active"

  final val DATA_TYPE_CONNECTION_CREATED = "Connection.created"
end ConnectionFinder
