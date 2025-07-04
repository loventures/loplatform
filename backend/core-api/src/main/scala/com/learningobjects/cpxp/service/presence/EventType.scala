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

package com.learningobjects.cpxp.service.presence

/** Typeclass describing a message that has an associated event type.
  *
  * @tparam A
  *   the message type
  */
trait EventType[A]:

  /** Return the associated event type.
    *
    * Dummy parameter to allow for `a.eventType`
    *
    * @param a
    *   the message
    * @return
    *   the event type
    */
  def eventType(a: A): String
end EventType

/** Typed message companion.
  */
object EventType:
  def apply[A: EventType]: EventType[A] = implicitly

  /** Return a fixed event type.
    *
    * @param eventType
    *   the event type
    * @tparam A
    *   the message type
    */
  def apply[A](eventType: String): EventType[A] = _ => eventType
end EventType
