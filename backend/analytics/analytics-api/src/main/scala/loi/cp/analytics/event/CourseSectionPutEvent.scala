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

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude}
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.entity.{ExternallyIdentifiableEntity, IntegrationData, SectionData}

/** Event emitted when a course section is created or updated. Do not emit this anymore. Replaced by SectionUpdateEvent
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class CourseSectionPutEvent3(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  section: SectionData,
  commitId: Long,
  offeringId: Long,
  startTime: Option[Date],
  endTime: Option[Date],
  subject: ExternallyIdentifiableEntity,
  integration: Option[IntegrationData]
) extends Event:

  override val eventType: String     = this.getClass.getSimpleName
  @JsonIgnore override val sessionId = Some(session)
  override val actionType            = EventActionType.PUT
end CourseSectionPutEvent3

/** older version (lacks section name) Do not emit this event anymore
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class CourseSectionPutEvent2(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  section: ExternallyIdentifiableEntity,
  commitId: Long,
  offeringId: Long,
  startTime: Option[Date],
  endTime: Option[Date],
  subject: ExternallyIdentifiableEntity,
  integration: Option[IntegrationData]
) extends Event:

  override val eventType: String     = this.getClass.getSimpleName
  @JsonIgnore override val sessionId = Some(session)
  override val actionType            = EventActionType.PUT
end CourseSectionPutEvent2

/** older version (lacks offeringId) Do not emit this event anymore
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class CourseSectionPutEvent(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  section: ExternallyIdentifiableEntity,
  commitId: Long,
  startTime: Option[Date],
  endTime: Option[Date],
  subject: ExternallyIdentifiableEntity,
  integration: Option[IntegrationData]
) extends Event:

  override val eventType: String     = this.getClass.getSimpleName
  @JsonIgnore override val sessionId = Some(session)
  override val actionType            = EventActionType.PUT
end CourseSectionPutEvent
