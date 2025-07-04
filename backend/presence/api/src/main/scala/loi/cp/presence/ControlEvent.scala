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

/** Control events are SSE events used to provide low-level control information to the SSE client.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed trait ControlEvent

/** Control event companion.
  */
object ControlEvent:

  /** The control event type. */
  final val Type = "Control"

  /** Typeclass evidence for the event type of control events. */
  implicit def ControlEventType[A <: ControlEvent]: EventType[A] = EventType(Type)

/** A control event to indicate the start of an SSE stream. */
@JsonTypeName("Start")
case object StartEvent extends ControlEvent

/** A control event to keep the SSE stream from idle timeout. */
@JsonTypeName("Heartbeat")
case object HeartbeatEvent extends ControlEvent

/** A control event to keep the SSE stream has ended. */
@JsonTypeName("SessionEnded")
case object SessionEndedEvent extends ControlEvent

/** A control event to alert that a new Announcement has been fired. */
@JsonTypeName("Announcement")
case class AnnouncementStart(id: Long, startTime: Date, endTime: Date, message: String, style: String)
    extends ControlEvent

/** A control event to alert that a new Announcement has been ended. */
@JsonTypeName("AnnouncementEnd")
case class AnnouncementEnd(id: Long) extends ControlEvent
