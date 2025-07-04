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

package loi.cp.assessment

import java.time.Instant
import argonaut.Argonaut.*
import argonaut.*
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.attempt.{AttemptState, UserAttemptCounts}
import loi.cp.assessment.settings.AttemptLimit
import loi.cp.context.ContextId
import loi.cp.reference.{ContentIdentifier, EdgePath}
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.*
import scaloi.syntax.collection.*

/** Overviews intended for instructors for assessments.
  */
object InstructorOverviews:

  /** Assessment overview information for grading a specific learner.
    *
    * @param identifier
    *   the assessment identifier
    * @param learner
    *   the learner whose overview data is being returned
    * @param mostRecentSubmission
    *   the latest update time for the specified assessment
    * @param gradeableAttempts
    *   all attempts that need to be graded for this learner and assessment
    * @param attemptCount
    *   the total number of all attempts for this learner and assessment
    * @param invalidAttemptCount
    *   the number of attempts that have been invalidated
    * @param hasViewableAttempts
    *   the learner has attempts the instructor can view
    * @param hasValidViewableAttempts
    *   the learner has attempts the instructor can grade
    * @param grade
    *   the grade for this assessment and learner
    */
  case class UserGradingOverview(
    identifier: ContentIdentifier,
    learner: UserDTO,
    mostRecentSubmission: Option[Instant],
    gradeableAttempts: Seq[AttemptId],
    attemptCount: AttemptCount,
    invalidAttemptCount: InvalidAttemptCount,
    hasViewableAttempts: Boolean,
    hasValidViewableAttempts: Boolean,
    grade: Option[Score]
  )

  /** An overview of the number of attempts a learner has against an assessment grouped by state.
    *
    * @param context
    *   the context the assessment is in
    * @param edgePath
    *   the location of the assessment
    * @param attemptCountsByLearnerId
    *   a map of subject id to the number of valid attempts by state
    */
  case class InstructorAttemptsOverview(
    context: ContextId,
    edgePath: EdgePath,
    attemptCountsByLearnerId: Map[Long, Map[AttemptState, Int]]
  ):

    /** Returns the attempt counts by state for the given learner. If the learner has no attempts, then
      * [[InstructorAttemptsOverview.emptyCounts]] are given.
      *
      * @param learnerId
      *   the learner to get counts for
      * @return
      *   the attempt counts by state; otherwise, [[InstructorAttemptsOverview.emptyCounts]]
      */
    def studentAttempts(learnerId: Long): Map[AttemptState, Int] =
      attemptCountsByLearnerId.getOrElse(learnerId, InstructorAttemptsOverview.emptyCounts)
  end InstructorAttemptsOverview

  object InstructorAttemptsOverview:
    val emptyCounts: Map[AttemptState, Int] = AttemptState.values.map(_ -> 0).toMap

    def of(counts: Seq[UserAttemptCounts], assessments: Seq[Assessment]): Seq[InstructorAttemptsOverview] =
      val studentCountsByQuiz: Map[(ContextId, EdgePath), Seq[UserAttemptCounts]] =
        counts.groupBy(count => (count.context, count.edgePath))

      for
        assessment    <- assessments
        studentCounts <- studentCountsByQuiz.get((assessment.contextId, assessment.edgePath)).toSeq
      yield
        val userAttemptCountByState: Map[Long, Map[AttemptState, Int]] =
          studentCounts.map(count => count.user -> count.attemptStateCounts).toMap

        InstructorAttemptsOverview(assessment.contextId, assessment.edgePath, userAttemptCountByState)
    end of
  end InstructorAttemptsOverview

  implicit val attemptStateCountsCodec: CodecJson[Map[AttemptState, Int]] =
    mapCodec(_.toString, AttemptState.withNameOption)

  implicit val instructorAttemptsOverviewCodex: CodecJson[InstructorAttemptsOverview] =
    casecodec3(InstructorAttemptsOverview.apply, ArgoExtras.unapply)(
      "context",
      "edgePath",
      "studentAttempts"
    )
end InstructorOverviews

/** Overviews intended for learners for assessments.
  */
object LearnerOverviews:

  /** An overview for a learner's attempts against an assessment.
    *
    * @param context
    *   the context the assessment is in
    * @param edgePath
    *   the location of the assessment
    * @param userId
    *   the id of the subject
    * @param latestSubmissionTime
    *   latest submission time of for the subject's attempts
    * @param openAttempts
    *   the number of open attempts for the subject
    * @param allAttempts
    *   the number of all open valid attempts for the subject
    * @param maxAttempts
    *   the attempt limit for the assessment
    */
  case class LearnerAttemptOverview(
    context: ContextId,
    edgePath: EdgePath,
    userId: Long,
    latestSubmissionTime: Option[Instant],
    openAttempts: Int,
    allAttempts: Int,
    maxAttempts: AttemptLimit
  )

  object LearnerAttemptOverview:
    def apply(countData: UserAttemptCounts, assessment: Assessment): LearnerAttemptOverview =
      LearnerAttemptOverview(
        countData.context,
        countData.edgePath,
        countData.user,
        countData.latestSubmissionTime,
        countData.attemptStateCounts.getOrElse(AttemptState.Open, 0),
        countData.attemptStateCounts.values.sum,
        assessment.maxAttempts
      )

    def of(counts: Seq[UserAttemptCounts], assessments: Seq[Assessment], user: Long): Seq[LearnerAttemptOverview] =
      val dataByEdgePath: Map[EdgePath, UserAttemptCounts] = counts.groupUniqBy(_.edgePath)
      for assessment <- assessments
      yield
        val counts: UserAttemptCounts =
          dataByEdgePath.getOrElse(assessment.edgePath, UserAttemptCounts.empty(assessment, user))
        LearnerAttemptOverview(counts, assessment)

    implicit val learnerAttemptOverviewCodec: CodecJson[LearnerAttemptOverview] =
      CodecJson.casecodec7(LearnerAttemptOverview.apply, ArgoExtras.unapply)(
        "context",
        "edgePath",
        "userId",
        "latestSubmissionTime",
        "openAttempts",
        "allAttempts",
        "maxAttempts"
      )
  end LearnerAttemptOverview
end LearnerOverviews
