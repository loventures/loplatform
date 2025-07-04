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

import java.util.Date
import java.lang as jl

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.SqlIndex
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
@SqlIndex("(parent_id, aggregationKey) WHERE del IS NULL")
class AlertFinder extends LeafEntity:

  @Column
  var aggregationKey: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var context: Item = scala.compiletime.uninitialized

  @Column
  var count: jl.Long = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var notification: NotificationFinder = scala.compiletime.uninitialized

  @Column
  var time: Date = scala.compiletime.uninitialized

  @Column
  var viewed: jl.Boolean = scala.compiletime.uninitialized
end AlertFinder

object AlertFinder:
  final val ITEM_TYPE_ALERT                 = "Alert"
  final val DATA_TYPE_ALERT_NOTIFICATION    = "Alert.notification"
  final val DATA_TYPE_ALERT_VIEWED          = "Alert.viewed"
  final val DATA_TYPE_ALERT_TIME            = "Alert.time"
  final val DATA_TYPE_ALERT_COUNT           = "Alert.count"
  final val DATA_TYPE_ALERT_AGGREGATION_KEY = "Alert.aggregationKey"
  final val DATA_TYPE_ALERT_CONTEXT         = "Alert.context"
