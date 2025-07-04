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

package loi.cp.analytics.event

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude}
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.entity.UserData

import java.time.Instant
import java.util.{Date, UUID}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class EnrollmentCreateEvent2(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  enrollmentId: Long,
  user: UserData,
  sectionId: Long,
  role: String,
  disabled: Boolean,
  startTime: Option[Instant],
  endTime: Option[Instant],
  dataSource: Option[String],
) extends Event:

  override val eventType: String           = "EnrollmentCreateEvent2"
  override val actionType: EventActionType = EventActionType.CREATE

  @JsonIgnore
  override val sessionId: Option[Long] = Some(session)
end EnrollmentCreateEvent2

// legacy event
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class EnrollmentCreateEvent1(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  enrollmentId: Long,
  userId: Long,
  sectionId: Long,
  role: String,
  disabled: Boolean,
  startTime: Option[Instant],
  endTime: Option[Instant],
  dataSource: Option[String],
) extends Event:

  override val eventType: String           = "EnrollmentCreateEvent1"
  override val actionType: EventActionType = EventActionType.CREATE

  @JsonIgnore
  override val sessionId: Option[Long] = Some(session)
end EnrollmentCreateEvent1

@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class EnrollmentUpdateEvent2(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  enrollmentId: Long,
  user: UserData,
  sectionId: Long,
  role: String,
  disabled: Boolean,
  startTime: Option[Instant],
  endTime: Option[Instant],
  dataSource: Option[String],
) extends Event:
  override val eventType: String           = "EnrollmentUpdateEvent2"
  override val actionType: EventActionType = EventActionType.UPDATE

  @JsonIgnore
  override val sessionId: Option[Long] = Some(session)
end EnrollmentUpdateEvent2

@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class EnrollmentUpdateEvent1(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  enrollmentId: Long,
  userId: Long,
  sectionId: Long,
  role: String,
  disabled: Boolean,
  startTime: Option[Instant],
  endTime: Option[Instant],
  dataSource: Option[String],
) extends Event:
  override val eventType: String           = "EnrollmentUpdateEvent1"
  override val actionType: EventActionType = EventActionType.UPDATE

  @JsonIgnore
  override val sessionId: Option[Long] = Some(session)
end EnrollmentUpdateEvent1

@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class EnrollmentDeleteEvent1(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  enrollmentId: Long,
) extends Event:

  override val eventType: String           = "EnrollmentDeleteEvent1"
  override val actionType: EventActionType = EventActionType.DELETE

  @JsonIgnore
  override val sessionId: Option[Long] = Some(session)
end EnrollmentDeleteEvent1
