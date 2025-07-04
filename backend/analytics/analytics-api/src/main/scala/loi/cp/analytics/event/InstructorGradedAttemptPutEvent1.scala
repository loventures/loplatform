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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import loi.cp.analytics.AnalyticsConstants.EventActionType

import java.time.Instant
import java.util.{Date, UUID}

/** Replaced by AttemptPutEvent1, do not emit this anymore.
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class InstructorGradedAttemptPutEvent1(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  attemptId: Long,
  userId: Long,
  sectionId: Long,
  edgePath: String,
  assetId: Long,
  state: String,
  valid: Boolean,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  scorerUserId: Option[Long],
  createTime: Instant,
  submitTime: Option[Instant],
  scoreTime: Option[Instant],
) extends Event:

  override val eventType: String           = "InstructorGradedAttemptPutEvent1"
  override val actionType: EventActionType = EventActionType.PUT

  @JsonIgnore override val sessionId: Option[Long] = Some(session)
end InstructorGradedAttemptPutEvent1
