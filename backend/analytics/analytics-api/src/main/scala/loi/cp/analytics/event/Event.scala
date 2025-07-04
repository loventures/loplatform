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

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import loi.cp.analytics.AnalyticsConstants.EventActionType

/** Required metadata about Analytics Events.
  *
  * Note that this is missing the event grammar's subject.action.object properties, because those details are
  * constructed by each subtype's `toJsonTriplet` method which will coerce flattened properties into the correct
  * analytics grammar entity.
  */
@JsonCreator
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, // MINIMAL_CLASS has a leading period
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "eventType",
  visible = true
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes(
  Array( // keep these in alphabetical order
    new Type(
      name = "CompetencyMasteryEvent",
      value = classOf[CompetencyMasteryEvent]
    ),
    new Type(
      name = "CompetencySetMasteryEvent",
      value = classOf[CompetencySetMasteryEvent]
    ),
    new Type(name = "CourseEntryEvent", value = classOf[CourseEntryEvent]),
    new Type(name = "SectionEntryEvent1", value = classOf[SectionEntryEvent1]),
    new Type(name = "SectionCreateEvent1", value = classOf[SectionCreateEvent1]),
    new Type(name = "SectionCreateEvent2", value = classOf[SectionCreateEvent2]),
    new Type(name = "SectionUpdateEvent1", value = classOf[SectionUpdateEvent1]),
    new Type(name = "SectionUpdateEvent2", value = classOf[SectionUpdateEvent2]),
    new Type(name = "SectionDeleteEvent1", value = classOf[SectionDeleteEvent1]),
    new Type(name = "CourseSectionPutEvent", value = classOf[CourseSectionPutEvent]),
    new Type(name = "CourseSectionPutEvent2", value = classOf[CourseSectionPutEvent2]),
    new Type(name = "CourseSectionPutEvent3", value = classOf[CourseSectionPutEvent3]),
    new Type(name = "EvidenceEvent", value = classOf[EvidenceEvent]),
    new Type(name = "GradePutEvent1", value = classOf[GradePutEvent1]),
    new Type(name = "GradeUnsetEvent", value = classOf[GradeUnsetEvent]),
    new Type(name = "LtiLaunchEvent", value = classOf[LtiLaunchEvent]),
    new Type(name = "PageNavEvent", value = classOf[PageNavEvent]),
    new Type(name = "QuestionViewedEvent", value = classOf[QuestionViewedEvent]),
    new Type(name = "SessionEvent", value = classOf[SessionEvent]),
    new Type(
      name = "OfferBranchEvent",
      value = classOf[OfferBranchEvent]
    ),
    new Type(
      name = "OfferBranchEvent2",
      value = classOf[OfferBranchEvent2]
    ),
    new Type(
      name = "OfferBranchEvent3",
      value = classOf[OfferBranchEvent3]
    ),
    new Type(
      name = "PublishContentEvent1",
      value = classOf[PublishContentEvent1]
    ),
    new Type(name = "SurveySubmissionEvent1", value = classOf[SurveySubmissionEvent1]),
    new Type(name = "SurveySubmissionEvent2", value = classOf[SurveySubmissionEvent2]),
    new Type(name = "TimeSpentEvent", value = classOf[TimeSpentEvent]),
    new Type(name = "TimeSpentEvent2", value = classOf[TimeSpentEvent2]),
    new Type(name = "DiscussionPostPutEvent1", value = classOf[DiscussionPostPutEvent1]),
    new Type(name = "QnaThreadPutEvent1", value = classOf[QnaThreadPutEvent1]),
    new Type(name = "EnrollmentCreateEvent1", value = classOf[EnrollmentCreateEvent1]),
    new Type(name = "EnrollmentCreateEvent2", value = classOf[EnrollmentCreateEvent2]),
    new Type(name = "EnrollmentUpdateEvent1", value = classOf[EnrollmentUpdateEvent1]),
    new Type(name = "EnrollmentUpdateEvent2", value = classOf[EnrollmentUpdateEvent2]),
    new Type(name = "EnrollmentDeleteEvent1", value = classOf[EnrollmentDeleteEvent1]),
    new Type(name = "InstructorGradedAttemptPutEvent1", value = classOf[InstructorGradedAttemptPutEvent1]),
    new Type(name = "InstructorSnapshotDayCreateEvent1", value = classOf[InstructorSnapshotDayCreatEvent1]),
    new Type(name = "AttemptPutEvent1", value = classOf[AttemptPutEvent1]),
    new Type(name = "ProgressPutEvent1", value = classOf[ProgressPutEvent1]),
    new Type(name = "ProgressPutEvent2", value = classOf[ProgressPutEvent2]),
    new Type(name = "TutorialViewEvent1", value = classOf[TutorialViewEvent1]),
    new Type(name = "UIUpgradeEvent1", value = classOf[UIUpgradeEvent1]),
    new Type(name = "UserObfuscateEvent1", value = classOf[UserObfuscateEvent1]),
  )
)
trait Event:
  val eventType: String
  val id: UUID
  val time: Date
  val source: String
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  val sessionId: Option[Long]

  val actionType: EventActionType
end Event

object Event:
  def sectionId(e: Event): Option[Long] =
    e match
      case e: CourseEntryEvent    => Some(e.course.section.id)
      case e: LtiLaunchEvent      => e.course.map(_.section.id)
      case e: PageNavEvent        => e.course.map(_.section.id)
      case e: QuestionViewedEvent => Some(e.course.section.id)
      case _                      => None
end Event
