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

package com.learningobjects.cpxp.async.async

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.async.Event

import java.util.Date

/** This event represents the status of an asynchronous operation.
  * @param guid
  * @param origin
  * @param status
  * @param body
  * @param channel
  * @param id
  * @param timestamp
  */
case class AsyncEvent(
  guid: String,
  origin: String,
  status: String, // todo: an enum?
  body: JsonNode,
  channel: String,
  id: Long,
  timestamp: Date
) extends Event
