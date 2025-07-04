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

package loi.cp.analytics.redshift

import cats.instances.list.*
import cats.instances.map.*
import cats.instances.set.*
import cats.kernel.{Monoid, Semigroup}
import cats.syntax.semigroup.*

import java.util.Date

case class RedshiftPayload(
  assets: List[RsAsset] = Nil,
  sectionEntries: List[RsSectionEntry] = Nil,
  sections: List[RsSection] = Nil,
  sectionDeletes: Set[Long] = Set.empty,
  sectionContents: List[RsSectionContent] = Nil,
  sessions: List[RsSession] = Nil,
  sessionEvents: List[RsSessionEvent] = Nil,
  users: List[RsUser] = Nil,
  grades: Map[RsGrade.Key, RsGrade] = Map.empty,
  unsetGrades: Set[RsGrade.Key] = Set.empty,
  surveyQuestionResponses: List[RsSurveyQuestionResponse] = Nil,
  timeSpentDiscrete: List[RsTimeSpentDiscrete] = Nil,
  discussionPosts: List[RsDiscussionPost] = Nil,
  enrollments: List[RsEnrollment] = Nil,
  enrollmentDeletes: Set[Long] = Set.empty,
  attempts: List[RsAttempt] = Nil,           // includes Open, Submitted, Finalized attempts
  submittedAttempts: List[RsAttempt] = Nil,  // only includes Submitted and Finalized attempts
  pageNavEvents: List[RsPageNavEvent] = Nil,
  progresses: List[RsProgress] = Nil,
  progressOverTimes: List[RsProgressOverTime] = Nil,
  instructorSnapshotDays: List[Date] = Nil,
  tutorialViews: List[RsTutorialView] = Nil,
  userObfuscations: List[Long] = Nil,
  activityKeys: Set[ContentKey] = Set.empty, // keys that will be added to the activity "queue"
  qnaThreads: List[RsQnaThread] = Nil,
):

  lazy val nonEmpty: Boolean = LazyList(
    assets,
    sectionContents,
    sectionEntries,
    sections,
    sectionDeletes,
    sessions,
    sessionEvents,
    users,
    grades,
    unsetGrades,
    surveyQuestionResponses,
    timeSpentDiscrete,
    discussionPosts,
    enrollments,
    enrollmentDeletes,
    attempts,
    submittedAttempts,
    pageNavEvents,
    progresses,
    progressOverTimes,
    instructorSnapshotDays,
    tutorialViews,
    userObfuscations,
    activityKeys,
    qnaThreads,
  ).exists(_.nonEmpty)
end RedshiftPayload

object RedshiftPayload:

  // When combining assessmentGrade maps, upon key collision always take the second param.
  // As this is not a commutative semigroup this is fine.
  // cats appears to lack an equivalent to scalaz.Semigroup.lastTaggedSemigroup
  private implicit val sighh: Semigroup[RsGrade] = Semigroup.last

  implicit val monoidForRedshiftPayload: Monoid[RedshiftPayload] = Monoid.instance(
    RedshiftPayload(),
    (a, b) =>
      RedshiftPayload(
        a.assets |+| b.assets,
        a.sectionEntries |+| b.sectionEntries,
        (a.sections |+| b.sections).filterNot(s => b.sectionDeletes.contains(s.id)),
        (a.sectionDeletes |+| b.sectionDeletes) -- b.sections.map(_.id).toSet,
        a.sectionContents |+| b.sectionContents,
        a.sessions |+| b.sessions,
        a.sessionEvents |+| b.sessionEvents,
        a.users |+| b.users,
        (a.grades |+| b.grades) -- b.unsetGrades,
        (a.unsetGrades |+| b.unsetGrades) -- b.grades.keySet,
        (a.surveyQuestionResponses |+| b.surveyQuestionResponses),
        a.timeSpentDiscrete |+| b.timeSpentDiscrete,
        a.discussionPosts |+| b.discussionPosts,
        (a.enrollments |+| b.enrollments).filterNot(e => b.enrollmentDeletes.contains(e.id)),
        (a.enrollmentDeletes |+| b.enrollmentDeletes) -- b.enrollments.map(_.id).toSet,
        a.attempts |+| b.attempts,
        a.submittedAttempts |+| b.submittedAttempts,
        a.pageNavEvents |+| b.pageNavEvents,
        a.progresses |+| b.progresses,
        a.progressOverTimes |+| b.progressOverTimes,
        a.instructorSnapshotDays |+| b.instructorSnapshotDays,
        a.tutorialViews |+| b.tutorialViews,
        a.userObfuscations |+| b.userObfuscations,
        a.activityKeys |+| b.activityKeys,
        a.qnaThreads |+| b.qnaThreads,
      )
  )
end RedshiftPayload
