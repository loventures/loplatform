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

package loi.cp.notification

import java.util.Date

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}
import com.learningobjects.cpxp.service.presence.EventType

/** A notification has been created for the user.
  *
  * @param notification
  *   the serialized notification
  */
@JsonSerialize(`using` = classOf[NotificationEventSerializer])
case class NotificationEvent(notification: String)

/** Notification event companion.
  */
object NotificationEvent:

  /** The notification event type. */
  final val Type = "Notification"

  /** Typeclass evidence for the event type of notification events. */
  implicit val NotificationEventType: EventType[NotificationEvent] = EventType(Type)

/** Custom serializer that unwraps the encapsulated notification. The normal `JsonValue` annotation cannot be used
  * because it conflicts with case class serialization.
  */
class NotificationEventSerializer extends JsonSerializer[NotificationEvent]:
  override def serialize(event: NotificationEvent, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeRaw(event.notification)

/** An alert has been created for the user.
  *
  * @param id
  *   the alert id
  * @param timestamp
  *   the alert timestamp
  * @param aggregationKey
  *   the alert aggregation key
  * @param context
  *   the alert context
  */
case class AlertEvent(
  id: Long,
  timestamp: Date,
  aggregationKey: Option[String],
  context: Option[Long]
) // should this carry a payload?

/** Alert event companion.
  */
object AlertEvent:

  /** The alert event type. */
  final val Type = "Alert"

  /** Typeclass evidence for the event type of alert events. */
  implicit val AlertEventType: EventType[AlertEvent] = EventType(Type)
