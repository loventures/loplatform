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
import loi.cp.analytics.AnalyticsConstants.{EventActionType, SESSION_KEY}
import loi.cp.analytics.entity.{ExternallyIdentifiableEntity, IntegrationData}

object OfferBranchEvent:

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  case class Assessment2(
    id: Long,
    name: UUID,
    title: String,
    typeId: String,
    edgePath: String,
    forCredit: Boolean,
    pointsPossible: java.math.BigDecimal
  )

  // events from Aug 2019 - Jan 2020 have neither typeId nor edgePath
  // events from Jan 2020 - July 2020 have typeId and edgePath but use
  // OfferBranchEvent(1) which uses this class
  // Assessment2 should be used for all future events.
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  case class Assessment1(
    id: Long,
    name: UUID,
    title: String,
    typeId: Option[String],
    edgePath: Option[String],
    forCredit: Boolean,
    pointsPossible: java.math.BigDecimal
  )

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  case class CourseSection2(
    id: Long,
    externalId: Option[String],
    name: String,
    commitId: Long,
    startTime: Option[Date],
    endTime: Option[Date],
    integration: Option[IntegrationData]
  )

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  case class CourseSection1(
    id: Long,
    externalId: Option[String],
    commitId: Long,
    startTime: Option[Date],
    endTime: Option[Date],
    integration: Option[IntegrationData]
  )
end OfferBranchEvent

/** Event that occurs when a branch is published or updated. Do not emit anymore Replaced by PublishContentEvent1
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class OfferBranchEvent3(
  id: UUID,
  time: Date,          // system time
  @JsonProperty(SESSION_KEY)
  session: Long,
  source: String,
  branchId: Long,
  commitId: Long,
  courseId: Long,
  courseName: UUID,
  courseTitle: String, // the authoring course title
  offeringId: Long,
  offeringGroupId: String,
  offeringName: String,
  assessments: List[OfferBranchEvent.Assessment2],
  courseSections: List[OfferBranchEvent.CourseSection2],
  user: ExternallyIdentifiableEntity
) extends Event:

  override val eventType: String           = this.getClass.getSimpleName
  @JsonIgnore override val sessionId       = Some(session)
  override val actionType: EventActionType = EventActionType.UPDATE
end OfferBranchEvent3

/** Older version that did not include section names. Do not emit anymore.
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class OfferBranchEvent2(
  id: UUID,
  time: Date,          // system time
  @JsonProperty(SESSION_KEY)
  session: Long,
  source: String,
  branchId: Long,
  commitId: Long,
  courseId: Long,
  courseName: UUID,
  courseTitle: String, // the authoring course title
  offeringId: Long,
  offeringGroupId: String,
  offeringName: String,
  assessments: List[OfferBranchEvent.Assessment2],
  courseSections: List[OfferBranchEvent.CourseSection1],
  user: ExternallyIdentifiableEntity
) extends Event:

  override val eventType: String           = this.getClass.getSimpleName
  @JsonIgnore override val sessionId       = Some(session)
  override val actionType: EventActionType = EventActionType.UPDATE
end OfferBranchEvent2

/** An older version of the event that did not have the offering id, groupid or name. This event should not be emitted
  * anymore.
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class OfferBranchEvent(
  id: UUID,
  time: Date,          // system time
  @JsonProperty(SESSION_KEY)
  session: Long,
  source: String,
  branchId: Long,
  commitId: Long,
  courseId: Long,
  courseName: UUID,
  courseTitle: String, // the authoring course title
  assessments: List[OfferBranchEvent.Assessment1],
  courseSections: List[OfferBranchEvent.CourseSection1],
  user: ExternallyIdentifiableEntity
) extends Event:

  override val eventType: String           = this.getClass.getSimpleName
  @JsonIgnore override val sessionId       = Some(session)
  override val actionType: EventActionType = EventActionType.UPDATE
end OfferBranchEvent
