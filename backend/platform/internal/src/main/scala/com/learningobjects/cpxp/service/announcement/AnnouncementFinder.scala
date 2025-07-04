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

package com.learningobjects.cpxp.service.announcement

import java.util.Date
import jakarta.persistence.{Column, Entity}
import java.lang as jl

import com.learningobjects.cpxp.entity.PeerEntity
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

/** The Announcement Entity.
  */
@Entity
@HCache(usage = READ_WRITE)
class AnnouncementFinder extends PeerEntity:

  @Column
  var startTime: Date = scala.compiletime.uninitialized

  @Column
  var endTime: Date = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  var message: String = scala.compiletime.uninitialized

  @Column
  var style: String = scala.compiletime.uninitialized

  @Column
  var active: jl.Boolean = scala.compiletime.uninitialized
end AnnouncementFinder

object AnnouncementFinder:
  final val ITEM_TYPE_ANNOUNCEMENT            = "Announcement"
  final val DATA_TYPE_ANNOUNCEMENT_START_TIME = "Announcement.startTime"
  final val DATA_TYPE_ANNOUNCEMENT_END_TIME   = "Announcement.endTime"
  final val DATA_TYPE_ANNOUNCEMENT_MESSAGE    = "Announcement.message"
  final val DATA_TYPE_ANNOUNCEMENT_STYLE      = "Announcement.style"
  final val DATA_TYPE_ANNOUNCEMENT_ACTIVE     = "Announcement.active"
