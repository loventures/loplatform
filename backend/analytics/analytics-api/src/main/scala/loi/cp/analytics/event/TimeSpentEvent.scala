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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.entity.{ExternallyIdentifiableEntity, UserData}

import java.util.{Date, UUID}

/** @param maintenance
  *   true if this event is for Redshift maintenance, null otherwise
  * @param originSectionId
  *   origin section if this event was replicated from another courseLink.1 course, null otherwise
  */
case class TimeSpentEvent2(
  id: UUID,
  time: Date,
  source: String,                        // domain
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  sessionId: Option[Long],
  user: UserData,
  context: ExternallyIdentifiableEntity, // Section
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  commitId: Option[Long],
  edgePath: Option[String],
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  assetId: Option[Long],
  durationSpent: Long,                   // millis
  maintenance: Option[Boolean],
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  originSectionId: Option[Long],
) extends Event:
  override val actionType: EventActionType = EventActionType.TIME_SPENT

  override val eventType: String = this.getClass.getSimpleName
end TimeSpentEvent2

/** An older version of the even that is not emitted anymore. Does not contain the assetId.
  */
case class TimeSpentEvent(
  id: UUID,
  time: Date,
  source: String,                        // domain
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  sessionId: Option[Long],
  user: UserData,
  context: ExternallyIdentifiableEntity, // Section
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  commitId: Option[Long],
  edgePath: Option[String],
  durationSpent: Long                    // millis
) extends Event:

  override val actionType: EventActionType = EventActionType.TIME_SPENT

  override val eventType: String = this.getClass.getSimpleName
end TimeSpentEvent
