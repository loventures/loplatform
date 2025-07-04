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

package com.learningobjects.cpxp.service.session

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class SessionFinder extends LeafEntity:

  @Column
  var created: Date = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var expires: Date = scala.compiletime.uninitialized

  @Column
  var ipAddress: String = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var lastAccess: Date = scala.compiletime.uninitialized

  @Column
  var nodeName: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  var properties: String = scala.compiletime.uninitialized

  @Column
  var remember: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var sessionId: String = scala.compiletime.uninitialized

  @Column
  var state: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var user: Item = scala.compiletime.uninitialized
end SessionFinder

object SessionFinder:
  final val ITEM_TYPE_SESSION             = "Session"
  final val DATA_TYPE_SESSION_STATE       = "Session.state"
  final val DATA_TYPE_SESSION_USER        = "Session.user"
  final val DATA_TYPE_SESSION_IP_ADDRESS  = "Session.ipAddress"
  final val DATA_TYPE_SESSION_PROPERTIES  = "Session.properties"
  final val DATA_TYPE_SESSION_CREATED     = "Session.created"
  final val DATA_TYPE_SESSION_REMEMBER    = "Session.remember"
  final val DATA_TYPE_SESSION_ID          = "Session.sessionId"
  final val DATA_TYPE_SESSION_NODE_NAME   = "Session.nodeName"
  final val DATA_TYPE_SESSION_LAST_ACCESS = "Session.lastAccess"
  final val DATA_TYPE_SESSION_EXPIRES     = "Session.expires"
end SessionFinder
