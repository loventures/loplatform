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

package com.learningobjects.cpxp.service.maintenanceWindow

import jakarta.persistence.{Column, Entity}
import java.util.Date
import java.lang as jl

import com.learningobjects.cpxp.entity.PeerEntity
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

/** Maintenance Window Entity. Each Maintenance Windowed is associated with an Announcement.
  */
@Entity
@HCache(usage = READ_WRITE)
class MaintenanceWindowFinder extends PeerEntity:

  @Column
  var startTime: Date = scala.compiletime.uninitialized

  @Column
  var duration: jl.Long = scala.compiletime.uninitialized

  @Column
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var announcementId: jl.Long = scala.compiletime.uninitialized
end MaintenanceWindowFinder

object MaintenanceWindowFinder:
  final val ITEM_TYPE_MAINTENANCE_WINDOW                 = "MaintenanceWindow"
  final val DATA_TYPE_MAINTENANCE_WINDOW_START_TIME      = "MaintenanceWindow.startTime"
  final val DATA_TYPE_MAINTENANCE_WINDOW_DURATION        = "MaintenanceWindow.duration"
  final val DATA_TYPE_MAINTENANCE_WINDOW_DISABLED        = "MaintenanceWindow.disabled"
  final val DATA_TYPE_MAINTENANCE_WINDOW_ANNOUNCEMENT_ID = "MaintenanceWindow.announcementId"
