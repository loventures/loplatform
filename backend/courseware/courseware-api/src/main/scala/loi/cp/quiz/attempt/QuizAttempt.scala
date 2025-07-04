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

import java.time.Instant
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.AttemptId
import loi.cp.assessment.attempt.{AssessmentAttempt, AttemptState}
import loi.cp.quiz.Quiz
import loi.cp.reference.ContentIdentifier

/** A single attempt against a {{Quiz}}.
  *
  * @param id
  *   the persistence id of this quiz
  * @param state
  *   the current state of the attempt
  * @param valid
  *   whether this attempt is still valid
  * @param createTime
  *   when this attempt was created
  * @param submitTime
  *   when this attempt was submitted
  * @param autoSubmitted
  *   was this auto-submitted
  * @param updateTime
  *   the last time the attempt was updated (this includes both responses AND scoring AND feedback)
  * @param scoreTime
  *   the last time the score was updated (this is the latest time of when the quiz is submitted or last manually
  *   graded)
  * @param maxMinutes
  *   max permitted minutes
  * @param score
  *   the overall score for this quiz (the semantic meaning of a score before it is submitted is undefined)
  * @param questions
  *   the in-order question usages for this attempt
  * @param responses
  *   the in-order responses to questions in this attempt
  * @param userId
  *   the id of the owner of this quiz
  * @param assessment
  *   the quiz this attempt is against
  * @param folderId
  *   the folder containing the attempt
  * @param rootId
  *   the domain of the attempt
  */
case class QuizAttempt(
  override val id: AttemptId,
  state: AttemptState,
  valid: Boolean,
  createTime: Instant,
  submitTime: Option[Instant],
  autoSubmitted: Boolean,
  updateTime: Instant,
  scoreTime: Option[Instant],
  maxMinutes: Option[Long],
  score: Option[QuizScore],
  questions: Seq[QuizAttemptQuestionUsage],
  responses: Seq[QuizQuestionResponse],
  user: UserDTO,
  assessment: Quiz,
  folderId: Long,
  rootId: Long
) extends AssessmentAttempt:
  override def getId: JLong = id.value

  override final val contentId: ContentIdentifier = assessment.contentId

  /** Returns whether this attempt needs a response for one or more responses needs a user selection.
    *
    * @return
    *   whether this attempt needs a response for one or more responses needs a user selection
    */
  def openResponses: Boolean = responses.exists(_.state.open)

  /** Returns whether this attempt needs manual intervention for scoring at least one responses.
    *
    * @return
    *   whether this attempt needs manual intervention for scoring at least one responses
    */
  def pendingGrading: Boolean = responses.exists(_.state == QuestionResponseState.ResponseSubmitted)

  override lazy val scorer: Option[Long] = responses.collectFirst({
    case QuizQuestionResponse(_, _, _, _, Some(scorer), _, _, _, _) => scorer
  })

  override def toString: String = s"QuizAttempt(${id.value}, $state, valid=$valid, user=${user.id})"
end QuizAttempt
