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
import loi.cp.analytics.entity.{ContentId, CourseId, ExternallyIdentifiableEntity, Score}

import java.util.{Date, UUID}

/** Event emitted when a grade is created or updated
  * @param maintenance
  *   true if this event is for Redshift maintenance, null otherwise
  */
case class GradePutEvent1(
  id: UUID,
  time: Date,
  source: String,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  sessionId: Option[Long],
  learner: ExternallyIdentifiableEntity,
  section: ExternallyIdentifiableEntity,
  edgePath: String,
  assetId: Long,
  forCredit: Boolean,
  score: Score,
  maintenance: Option[Boolean]
) extends Event:

  override val eventType: String           = "GradePutEvent1"
  override val actionType: EventActionType = EventActionType.PUT
end GradePutEvent1

/** Event emitted when a grade is deleted, such as an instructor invalidating all finalized attempts.
  * @param maintenance
  *   true if this event is for Redshift maintenance, null otherwise
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class GradeUnsetEvent(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  course: CourseId,
  contentId: ContentId,
  learner: ExternallyIdentifiableEntity,
  subject: ExternallyIdentifiableEntity,
  maintenance: Option[Boolean]
) extends Event:

  override val eventType: String                   = "GradeUnsetEvent"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType                          = EventActionType.SAVE

end GradeUnsetEvent
