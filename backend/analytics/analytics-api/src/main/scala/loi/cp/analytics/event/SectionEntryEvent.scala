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

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude, JsonProperty}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import loi.cp.analytics.AnalyticsConstants.{EventActionType, SESSION_KEY}
import loi.cp.analytics.entity.UserData

import java.util.{Date, UUID}

/** Records entry into a section.
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class SectionEntryEvent1(
  id: UUID,
  time: Date,
  source: String,
  @JsonProperty(SESSION_KEY)
  session: Long,
  user: UserData,
  role: String,
  sectionId: Long,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  originSectionId: Option[Long],
) extends Event:

  override val eventType: String = this.getClass.getSimpleName
  override val actionType        = EventActionType.ENTER

  @JsonIgnore override val sessionId: Option[Long] = Some(session)
end SectionEntryEvent1
