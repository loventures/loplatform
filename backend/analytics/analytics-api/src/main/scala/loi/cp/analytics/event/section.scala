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

import java.time.Instant
import java.util.{Date, UUID}

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude}
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.entity.{IntegrationData, SectionData}

/** Event emitted when a section is created.
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class SectionCreateEvent2(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  section: SectionData,
  offeringId: Long,
  offeringGroupId: String,
  offeringName: String,
  contents: List[PublishContentEvent.Content1],
  integration: Option[IntegrationData],
  disabled: Boolean,
  startDate: Option[Instant],
  endDate: Option[Instant],
  groupId: Option[String],
) extends Event:

  override val eventType: String                   = "SectionCreateEvent2"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType                          = EventActionType.CREATE
end SectionCreateEvent2

/** Legacy event, did not have `disabled`, `starttime`, `endtime`
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class SectionCreateEvent1(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  section: SectionData,
  offeringId: Long,
  offeringGroupId: String,
  offeringName: String,
  contents: List[PublishContentEvent.Content1],
  integration: Option[IntegrationData]
) extends Event:

  override val eventType: String                   = "SectionCreateEvent1"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType                          = EventActionType.CREATE
end SectionCreateEvent1

/** Event emitted when a section is updated. A section update is when properties of the section, excluding its content,
  * are updated (e.g. name, externalId). Content updates occur under different workflows and are recorded by
  * [[PublishContentEvent]]
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class SectionUpdateEvent2(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  section: SectionData,
  offeringId: Long,
  offeringGroupId: String,
  offeringName: String,
  integration: Option[IntegrationData],
  disabled: Boolean,
  startDate: Option[Instant],
  endDate: Option[Instant],
  groupId: Option[String],
) extends Event:

  override val eventType: String                   = "SectionUpdateEvent2"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType                          = EventActionType.UPDATE
end SectionUpdateEvent2

/** Legacy event, did not have `disabled`, `starttime`, `endtime`
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class SectionUpdateEvent1(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  section: SectionData,
  offeringId: Long,
  offeringGroupId: String,
  offeringName: String,
  integration: Option[IntegrationData]
) extends Event:

  override val eventType: String                   = "SectionUpdateEvent1"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType                          = EventActionType.UPDATE
end SectionUpdateEvent1

/** Event emitted when a section is deleted.
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class SectionDeleteEvent1(
  id: UUID,
  session: Long,
  source: String,
  time: Date,
  sectionId: Long,
) extends Event:
  override val eventType: String                   = "SectionDeleteEvent1"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType                          = EventActionType.DELETE
end SectionDeleteEvent1
