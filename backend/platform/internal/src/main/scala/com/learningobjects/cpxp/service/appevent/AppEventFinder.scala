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

package com.learningobjects.cpxp.service.appevent

import java.util.Date

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*

@Entity
@SqlIndex(name = "appeventfinder_general_idx", value = "(deadline, id, fired) where del is null and state is null")
class AppEventFinder extends LeafEntity:

  @Column
  var created: Date = scala.compiletime.uninitialized

  @Column
  var deadline: Date = scala.compiletime.uninitialized

  @Column
  var started: Date = scala.compiletime.uninitialized

  @Column
  var finished: Date = scala.compiletime.uninitialized

  @Column
  @FriendlyName
  var eventId: String = scala.compiletime.uninitialized

  @Column
  var fired: Date = scala.compiletime.uninitialized

  @Column
  var host: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  var payload: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var rel0: Item = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var rel1: Item = scala.compiletime.uninitialized

  @Column
  var state: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var target: Item = scala.compiletime.uninitialized
end AppEventFinder

object AppEventFinder:
  final val ITEM_TYPE_APP_EVENT          = "AppEvent"
  final val DATA_TYPE_APP_EVENT_TARGET   = "AppEvent.target"
  final val DATA_TYPE_APP_EVENT_EVENT_ID = "AppEvent.eventId"
  final val DATA_TYPE_APP_EVENT_CREATED  = "AppEvent.created"
  final val DATA_TYPE_APP_EVENT_REL_1    = "AppEvent.rel1"
  final val DATA_TYPE_APP_EVENT_REL_0    = "AppEvent.rel0"
  final val DATA_TYPE_APP_EVENT_HOST     = "AppEvent.host"
  final val DATA_TYPE_APP_EVENT_PAYLOAD  = "AppEvent.payload"
  final val DATA_TYPE_APP_EVENT_STATE    = "AppEvent.state"
  final val DATA_TYPE_APP_EVENT_FIRED    = "AppEvent.fired"
  final val DATA_TYPE_APP_EVENT_DEADLINE = "AppEvent.deadline"
  final val DATA_TYPE_APP_EVENT_STARTED  = "AppEvent.started"
  final val DATA_TYPE_APP_EVENT_FINISHED = "AppEvent.finished"
end AppEventFinder
