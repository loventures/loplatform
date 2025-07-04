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

@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class QuestionViewedEvent(
  id: UUID,
  time: Date,
  source: String,
  @JsonProperty(SESSION_KEY)
  session: Long,
  clientTime: Date,
  user: ExternallyIdentifiableEntity,
  question: UUID,
  assessment: ContentId,
  course: CourseId
) extends Event:

  override val eventType: String     = this.getClass.getSimpleName
  override val actionType            = EventActionType.SAVE
  @JsonIgnore override val sessionId = Some(session)

end QuestionViewedEvent
