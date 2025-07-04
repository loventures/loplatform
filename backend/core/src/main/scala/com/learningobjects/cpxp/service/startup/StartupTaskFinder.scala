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

package com.learningobjects.cpxp.service.startup

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class StartupTaskFinder extends DomainEntity:
  import StartupTaskFinder.*

  @Column(nullable = false)
  var identifier: String = scala.compiletime.uninitialized

  @Column(nullable = false, columnDefinition = "TEXT")
  var logs: String = scala.compiletime.uninitialized

  @Column(nullable = false)
  var state: String = scala.compiletime.uninitialized

  @Column(nullable = false)
  var timestamp: Date = scala.compiletime.uninitialized

  @Column(nullable = false)
  @DataType(DATA_TYPE_STARTUP_TASK_VERSION)
  var startupTaskVersion: jl.Long = scala.compiletime.uninitialized
end StartupTaskFinder

object StartupTaskFinder:
  final val ITEM_TYPE_STARTUP_TASK            = "StartupTask"
  final val DATA_TYPE_STARTUP_TASK_VERSION    = "StartupTask.version"
  final val DATA_TYPE_STARTUP_TASK_TIMESTAMP  = "StartupTask.timestamp"
  final val DATA_TYPE_STARTUP_TASK_IDENTIFIER = "StartupTask.identifier"
  final val DATA_TYPE_STARTUP_TASK_STATE      = "StartupTask.state"
  final val DATA_TYPE_STARTUP_TASK_LOGS       = "StartupTask.logs"
