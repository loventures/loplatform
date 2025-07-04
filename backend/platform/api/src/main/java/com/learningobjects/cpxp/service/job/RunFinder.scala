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

package com.learningobjects.cpxp.service.job

import java.util.Date
import java.lang as jl

import com.learningobjects.cpxp.entity.*
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class RunFinder extends PeerEntity:

  @Column(columnDefinition = "TEXT")
  var reason: String = scala.compiletime.uninitialized

  @Column
  var success: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var startTime: Date = scala.compiletime.uninitialized

  @Column
  var stopTime: Date = scala.compiletime.uninitialized
end RunFinder

object RunFinder:
  final val ITEM_TYPE_RUN            = "Run"
  final val DATA_TYPE_RUN_REASON     = "Run.reason"
  final val DATA_TYPE_RUN_SUCCESS    = "Run.success"
  final val DATA_TYPE_RUN_START_TIME = "Run.startTime"
  final val DATA_TYPE_RUN_STOP_TIME  = "Run.stopTime"
