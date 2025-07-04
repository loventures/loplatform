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

package com.learningobjects.cpxp.async

import java.util.Date

/** Events are serializable representations of events for use through the event system.
  *
  * Events extends Product so that it export information about the arity of it's constructors in it's type. Exporting
  * this information is done automatically if the the implementing class is a scala case class.
  */
trait Event extends Product:
  def channel: ChannelId
  def id: Long
  def timestamp: Date
