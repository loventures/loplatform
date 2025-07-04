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

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.SqlIndex
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
@SqlIndex("(parent_id, time DESC) WHERE del IS NULL")
class NotifyFinder extends LeafEntity:

  @ManyToOne(fetch = FetchType.LAZY)
  var notification: NotificationFinder = scala.compiletime.uninitialized

  @Column
  var time: Date = scala.compiletime.uninitialized
end NotifyFinder

object NotifyFinder:
  final val ITEM_TYPE_NOTIFY              = "Notify"
  final val DATA_TYPE_NOTIFY_TIME         = "Notify.time"
  final val DATA_TYPE_NOTIFY_NOTIFICATION = "Notify.notification"
