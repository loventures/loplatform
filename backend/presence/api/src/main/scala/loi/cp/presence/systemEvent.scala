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

package loi.cp.presence

import java.util.Date

import com.fasterxml.jackson.annotation.{JsonTypeInfo, JsonTypeName}
import com.learningobjects.cpxp.service.presence.EventType

/** System events.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed trait SystemEvent

/** System event companion.
  */
object SystemEvent:

  /** The system event type. */
  final val Type = "System"

  /** Typeclass evidence for the event type of system events. */
  implicit def SystemEventType[A <: SystemEvent]: EventType[A] = EventType(Type)

/** The system is going into maintenance mode.
  *
  * @param reason
  *   the maintenance mode reason
  * @param end
  *   when maintenance is expected to end
  */
@JsonTypeName("Maintenance")
case class MaintenanceEvent(reason: String, end: Option[Date]) extends SystemEvent

/** You logged out of your current HTTP session.
  */
@JsonTypeName("Logout")
case object LogoutEvent extends SystemEvent

/** You logged in on your current HTTP session.
  */
@JsonTypeName("Login")
case object LoginEvent extends SystemEvent
