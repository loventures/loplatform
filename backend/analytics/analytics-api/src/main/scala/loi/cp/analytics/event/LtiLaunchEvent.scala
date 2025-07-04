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

import java.util.{Date, UUID}

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude, JsonProperty}
import loi.cp.analytics.AnalyticsConstants.*
import loi.cp.analytics.entity.{ContentId, CourseId, ExternallyIdentifiableEntity}

/** Captures events related to LTI tool launches Subjects: Person Actions: LAUNCHED Objects: LTI Tool
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class LtiLaunchEvent(
  id: UUID,
  time: Date,
  source: String,
  @JsonProperty(SESSION_KEY)
  session: Long,
  user: ExternallyIdentifiableEntity,
  toolId: String,
  url: Option[String],
  content: Option[ContentId], // blank for global launches
  course: Option[CourseId],
  isGraded: Option[Boolean],
  ltiVersion: Option[String],
  ltiMessageType: Option[String],
  customParameters: Option[Map[String, String]],
) extends Event:
  override val actionType: EventActionType = EventActionType.LAUNCHED
  override val eventType: String           = this.getClass.getSimpleName
  @JsonIgnore override val sessionId       = Some(session)

end LtiLaunchEvent
