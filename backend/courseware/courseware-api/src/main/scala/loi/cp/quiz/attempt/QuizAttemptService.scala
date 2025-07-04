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

package loi.cp.quiz.attempt

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
import loi.cp.quiz.Quiz
import loi.cp.quiz.attempt.DistractorOrder.AuthoredResponseSelection
import loi.cp.quiz.attempt.QuizAttemptService.{AttachmentRequest, SelectionRequest}
import loi.cp.quiz.question.Question
import loi.cp.reference.EdgePath
import scalaz.\/

import scala.concurrent.duration.*

/** A service for getting and creating {{QuizAttempt}}s.
  */
@Service
trait QuizAttemptService:

  /** Returns all attempts against the given quizzes in the given course. This includes invalidated attempts.
    *
    * @param course
    *   the course containing the quizzes
    * @param quizzes
    *   the quizzes to get attempts for
    * @return
    *   all attempts for the given quizzes
    */
  // TODO: I.. I am a bad API.. I scale linearly... And you? You are just being lazy.
  def getAttempts(course: CourseSection, quizzes: Seq[Quiz]): Seq[QuizAttempt]

  /** Returns attempts against the given quizzes made by a single user. This includes invalidated attempts.
    *
    * @param course
    *   the course containing the quizzes
    * @param quizzes
    *   the quizzes to get attempts for
    * @param userId
    *   the subject to query for
    * @return
    *   all attempts the user made against the given {{quizzes}}
    */
  def getUserAttempts(course: CourseSection, quizzes: Seq[Quiz], user: UserDTO): Seq[QuizAttempt]

  def countValidAttempts(course: CourseSection, quizzes: Seq[Quiz]): Int

  def countValidAttempts(course: CourseSection, quiz: Quiz, userId: UserId): Int

  /** Creates a new attempt for the given {{user}} against the given {{quiz}}.
    *
    * @param quiz
    *   the quiz to create the attempt against
    * @param user
    *   the user taking the attempt
    * @return
    *   the new attempt if successful
    */
  def createAttempt(
    quiz: Quiz,
    questions: Seq[Question],
    user: UserDTO
  ): AttemptLimitExceeded \/ QuizAttempt

  /** Mark the attempt as submitted, and close any open responses. If all responses are scored, update the score for the
    * attempt.
    *
    * @param attempt
    *   the attempt that is being submitted
    * @return
    *   the submitted attempt
    */
  def submitAttempt(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt,
    autoSubmit: Boolean = false,
  ): QuizAttemptFailure \/ QuizAttempt

  /** Process all test out gates. */
  def testOut(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt
  ): QuizAttemptFailure \/ List[EdgePath]

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
  def invalidateAttempt(attempt: QuizAttempt): QuizAttemptFailure \/ QuizAttempt

  /** Submit a group of responses together.
    *
    * @param attempt
    *   the attempt
    * @param selectionRequests
    *   responses being made against the attempt
    * @param attachmentRequests
    *   attachments being to be put in the attempt
    * @param submitResponse
    *   submit the attempt even if not all questions are responded
    * @param autoSubmit
    *   this was the result of an auto-submit event
    * @return
    *   the updated attempt
    */
  def respond(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt,
    selectionRequests: Seq[SelectionRequest],
    attachmentRequests: Seq[AttachmentRequest],
    submitResponse: Boolean = false,
    autoSubmit: Boolean = false,
  ): QuizAttemptFailure \/ QuizAttempt

  /** Sets a score for a learner response, specified by question index. This does not mark the grade as the official
    * grade for the response.
    *
    * @param attempt
    *   the attempt
    * @param questionIndex
    *   the index of the response the score is going against
    * @param score
    *   the score for the response
    * @param scorer
    *   the user applying the score; or [[None]] if the system is scoring the response
    * @return
    *   The updated quiz attempt, or an exception specifying why the response could not be accepted.
    */
  def draftResponseScore(
    attempt: QuizAttempt,
    questionIndex: Int,
    score: Option[ResponseScore],
    scorer: Option[Long]
  ): QuizAttemptFailure \/ QuizAttempt

  /** Submits a score for a learner response, specified by question index. This advances the response to score
    * submitted.
    *
    * @param attempt
    *   the attempt
    * @param questionIndex
    *   the index of the response the score is going against
    * @param score
    *   the score for the response
    * @param scorer
    *   the user applying the score; or [[None]] if the system is scoring the response
    * @return
    *   The updated quiz attempt, or an exception specifying why the response could not be accepted.
    */
  def submitResponseScore(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt,
    questionIndex: Int,
    score: ResponseScore,
    scorer: Option[Long]
  ): QuizAttemptFailure \/ QuizAttempt

  /** Sets feedback on a response to a particular question in an attempt.
    *
    * @param attempt
    *   the attempt
    * @param questionIndex
    *   the index of the response the feedback is against
    * @param feedback
    *   the feedback
    * @param release
    *   whether or not `feedback` is released to the learner
    * @return
    *   The updated quiz attempt, or an exception specifying why the response could not be accepted.
    */
  def setResponseFeedback(
    attempt: QuizAttempt,
    questionIndex: Int,
    feedback: Seq[Feedback],
    release: Boolean,
  ): QuizAttemptFailure \/ QuizAttempt

  /** Return a quiz attempt based on its id.
    *
    * @param attemptId
    *   the id of the attempt
    * @return
    *   a quiz attempt based on its id
    */
  def fetch(context: CourseSection, attemptId: Long, policies: List[CourseAssessmentPolicy]): Option[QuizAttempt]

  /** Locks and fetches a quiz attempt based on its id.
    *
    * @param attemptId
    *   the id of the attempt
    * @return
    *   a locked quiz attempt based on its id
    */
  def fetchForUpdate(
    context: CourseSection,
    attemptId: Long,
    policies: List[CourseAssessmentPolicy]
  ): Option[QuizAttempt]

  /** Returns aggregations against a collection of quizzes for all users.
    *
    * @param course
    *   the course containing the quizzes
    * @param quizzes
    *   the quizzes to fetch aggregations for
    * @return
    *   aggregations for the attempts for the quizzes
    */
  def getParticipationData(course: CourseSection, quizzes: Seq[Quiz]): Seq[AssessmentParticipationData]

  /** Returns an aggregation of attempt for a collection of quizzes for a given user.
    *
    * @param course
    *   the course containing the quizzes
    * @param quizzes
    *   the quizzes to fetch aggregations for
    * @param userId
    *   the id of the subject to aggregate for
    * @return
    *   a per quiz per user aggregation of attempts
    */
  def getLearnerAttemptOverviews(course: CourseSection, quizzes: Seq[Quiz], userId: UserId): Seq[LearnerAttemptOverview]

  /** Returns an aggregation of attempt for a collection of quizzes for all users.
    *
    * @param course
    *   the course containing the quizzes
    * @param quizzes
    *   the quizzes to fetch aggregations for
    * @return
    *   a per quiz per user aggregation of attempts
    */
  def getInstructorAttemptsOverviews(course: CourseSection, quizzes: Seq[Quiz]): Seq[InstructorAttemptsOverview]

  def invalidateAttempts(
    course: CourseSection,
    edgePaths: Seq[EdgePath],
    user: UserId,
  ): Unit

  /** Changes the course id of each attempt for a given user to the destinationContextId.
    */
  def transferAttempts(
    course: CourseSection,
    edgePaths: Seq[EdgePath],
    user: UserId,
    destinationContextId: ContextId
  ): Unit
end QuizAttemptService

object QuizAttemptService:
  case class SelectionRequest(questionIndex: Int, selection: Option[AuthoredResponseSelection], submitResponse: Boolean)

  case class AttachmentRequest(questionIndex: Int, attachments: Seq[AttachmentId])

  val AutoSubmitGracePeriod: FiniteDuration = 15.seconds
