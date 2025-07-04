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

package loi.cp.submissionassessment.attempt

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.assessment.InstructorOverviews.InstructorAttemptsOverview
import loi.cp.assessment.LearnerOverviews.LearnerAttemptOverview
import loi.cp.assessment.attempt.AssessmentParticipationData
import loi.cp.assessment.{CourseAssessmentPolicy, Feedback, ResponseScore}
import loi.cp.attachment.AttachmentId
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.reference.EdgePath
import loi.cp.submissionassessment.SubmissionAssessment
import scalaz.\/

/** A service for getting and creating [[SubmissionAttempt]].
  */
@Service
trait SubmissionAttemptService:

  /** Returns all attempts against the given submission assessment in the given course. This includes invalidated
    * attempts.
    *
    * @param course
    *   the course containing the submission assessments
    * @param assessments
    *   the submission assessments to get attempts for
    * @return
    *   all attempts for the given submission attempts
    */
  def getAttempts(course: CourseSection, assessments: Seq[SubmissionAssessment]): Seq[SubmissionAttempt]

  /** Returns attempts against the given submission assessment made by a single subject. This includes invalidated
    * attempts.
    *
    * @param course
    *   the course containing the submission assessments
    * @param assessments
    *   the submission assessments to get attempts for
    * @param userId
    *   the subject to query for
    * @return
    *   all attempts the user made against the given {{assessments}}
    */
  def getUserAttempts(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment],
    user: UserDTO
  ): Seq[SubmissionAttempt]

  def countValidAttempts(course: CourseSection, submissionAssessments: Seq[SubmissionAssessment]): Int

  def countValidAttempts(course: CourseSection, submissionAssessment: SubmissionAssessment, userId: UserId): Int

  /** Creates a new attempt for the given {{user}} against the given [[SubmissionAssessment]] with a set of contextual
    * configurations.
    *
    * @param assessment
    *   the submission assessment to create the attempt against
    * @param subject
    *   the user the attempt is being created for
    * @param driver
    *   the user driving this request
    * @return
    *   the new attempt
    */
  def createAttempt(
    assessment: SubmissionAssessment,
    subject: UserDTO,
    driver: UserId
  ): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Sets the user's response for this attempt, overwriting any existing response.
    *
    * @param attempt
    *   the attempt to respond to
    * @param essay
    *   a textual response to the assessment
    * @param attachments
    *   the attachments for this attempt
    * @param driver
    *   the user driving this request
    * @return
    *   the updated attempt
    */
  def respond(
    attempt: SubmissionAttempt,
    essay: Option[String],
    attachments: Seq[AttachmentId],
    driver: UserId
  ): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Mark the attempt as submitted and closes it. The attempt is then ready to be graded.
    *
    * @param attempt
    *   the attempt that is being submitted
    * @param driver
    *   the user driving this request
    * @return
    *   the submitted attempt
    */
  def submitAttempt(attempt: SubmissionAttempt, driver: UserId): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Invalidates a single attempt. Invalidated attempts no longer count towards score and do not show up in the
    * student's list of attempts (nor count towards their attempt limit).
    *
    * This call will fail if the attempt is already invalidated.
    *
    * @param attempt
    *   the attempt to invalidate
    * @return
    *   the invalidated attempt
    */
  def invalidateAttempt(attempt: SubmissionAttempt): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Sets a score for an attempt. This does not mark the grade as the official grade for the response.
    *
    * @param attempt
    *   the attempt to score
    * @param score
    *   the score for the attempt
    * @param scorer
    *   the user applying the score
    * @return
    *   The updated assessment attempt, or an exception specifying why the response could not be accepted.
    */
  def setScore(
    attempt: SubmissionAttempt,
    score: Option[ResponseScore],
    scorer: UserDTO
  ): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Submits a score for a learner response, specified by question index. This advances the response to score
    * submitted.
    *
    * @param attempt
    *   the attempt to score
    * @return
    *   The updated assessment attempt, or an exception specifying why the response could not be accepted.
    */
  def submitScore(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: SubmissionAttempt
  ): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Reopens the attempt for scoring. This reverts the attempt into a submitted state. External systems are informed
    * that the attempt is being reopened. Only assessment attempts in
    * [[loi.cp.assessment.attempt.AttemptState.Finalized]] state may be reopened.
    *
    * @param attempt
    *   the attempt to reopen
    * @param scorer
    *   the user reopening the attempt for scoring
    * @return
    *   The updated attempt, or an exception specifying why the attempt could not be reopened.
    */
  def reopenAttempt(attempt: SubmissionAttempt, scorer: Long): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Drafts, but does not release, feedback to the attempt.
    *
    * @param attempt
    *   the attempt to provide feedback to
    * @param feedback
    *   the draft feedback
    * @return
    *   The updated attempt, or an exception specifying why the feedback could not be accepted.
    */
  def draftFeedback(attempt: SubmissionAttempt, feedback: Seq[Feedback]): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Submits and releases feedback to an attempt. This finalizes the attempt.
    *
    * @param attempt
    *   the attempt to provide feedback to
    * @param feedback
    *   the feedback
    * @return
    *   The updated assessment attempt, or an exception specifying why the response could not be accepted.
    */
  def submitFeedback(attempt: SubmissionAttempt, feedback: Seq[Feedback]): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Return a submission assessment attempt based on its id.
    *
    * @param attemptId
    *   the attempt to get
    * @return
    *   a submission assessment attempt based on its id
    */
  def fetch(
    context: CourseSection,
    ws: AttachedReadWorkspace,
    attemptId: Long,
    policies: List[CourseAssessmentPolicy]
  ): Option[SubmissionAttempt]

  /** Return a locked submission assessment attempt based on its id.
    *
    * @param attemptId
    *   the attempt to get
    * @return
    *   a locked submission assessment attempt based on its id
    */
  def fetchForUpdate(
    context: CourseSection,
    ws: AttachedReadWorkspace,
    attemptId: Long,
    policies: List[CourseAssessmentPolicy]
  ): Option[SubmissionAttempt]

  /** Returns aggregations against a collection of submission assessments for all users.
    *
    * @param course
    *   the course containing the assessments
    * @param assessments
    *   the assessments to fetch aggregations for
    * @return
    *   aggregations for the attempts for the submission assessments
    */
  def getParticipationData(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment]
  ): Seq[AssessmentParticipationData]

  /** Returns an aggregation of attempt for a collection of assessments for a given user.
    *
    * @param course
    *   the course containing the assessments
    * @param assessments
    *   the assessments to fetch aggregations for
    * @param userId
    *   the id of the subject to aggregate for
    * @return
    *   a per quiz per user aggregation of attempts
    */
  def getLearnerAttemptOverviews(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment],
    userId: UserId
  ): Seq[LearnerAttemptOverview]

  /** Returns an aggregation of attempt for a collection of assessments for all users.
    *
    * @param course
    *   the course containing the assessments
    * @param assessments
    *   the assessments to fetch aggregations for
    * @return
    *   a per quiz per user aggregation of attempts
    */
  def getInstructorAttemptsOverviews(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment]
  ): Seq[InstructorAttemptsOverview]

  def invalidateAttempts(
    course: CourseSection,
    edgePaths: Seq[EdgePath],
    subject: UserId,
  ): Unit

  /** Changes the course id of each attempt for a given subject to the destinationContextId.
    */
  def transferAttempts(
    course: CourseSection,
    edgePaths: Seq[EdgePath],
    subject: UserId,
    destinationContextId: ContextId
  ): Unit
end SubmissionAttemptService
