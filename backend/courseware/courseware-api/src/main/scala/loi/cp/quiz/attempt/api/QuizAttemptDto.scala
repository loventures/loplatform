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

package loi.cp.quiz.attempt.api

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import loi.cp.assessment.attempt.AttemptState
import loi.cp.assessment.{AttemptId, Feedback, ResponseScore}
import loi.cp.attachment.{AttachmentId, AttachmentInfo}
import loi.cp.quiz.attempt.DistractorOrder.DisplayResponseSelection
import loi.cp.quiz.attempt.{DistractorOrder, QuestionResponseState, QuizQuestionResponse, QuizScore}
import loi.cp.quiz.question.api.QuestionDto
import loi.cp.reference.ContentIdentifier

import java.time.Instant
import scala.util.{Failure, Success, Try}

/** A REST object representing a quiz attempt.
  *
  * @param id
  *   the persistence id for this quiz
  * @param questions
  *   the questions for this attempt
  * @param responses
  *   the current responses in this attempt
  * @param contentId
  *   the identifier for the quiz this attempt is against
  * @param state
  *   the current state of this attempt
  * @param createTime
  *   the time this attempt was created
  * @param submitTime
  *   the time this attempt was submitted, if it has been
  * @param autoSubmitted
  *   was it auto-submitted
  * @param remainingMillis
  *   how many milliseconds are left on this attempt
  * @param score
  *   the associated attempt score, if the attempt has been finalized
  * @param valid
  *   the flag indicating if attempt was invalidated
  * @param attachments
  *   information on all attachments referenced by this attempt
  */
case class QuizAttemptDto(
  id: AttemptId,
  questions: Seq[QuestionDto],
  responses: Seq[QuestionResponseDto],
  contentId: ContentIdentifier,
  state: AttemptState,
  createTime: Instant,
  submitTime: Option[Instant],
  autoSubmitted: Boolean,
  remainingMillis: Option[Long],
  score: Option[QuizScore],
  valid: Boolean,
  attachments: Map[Long, AttachmentInfo],
)

case class QuestionResponseDto(
  selection: Option[DisplayResponseSelection],
  attachments: Seq[AttachmentId],
  state: QuestionResponseState,
  score: Option[ResponseScore],
  @JsonDeserialize(contentAs = classOf[JLong]) scorer: Option[Long],
  selectionTime: Option[Instant],
  scoreTime: Option[Instant],
  instructorFeedback: Seq[Feedback],
  instructorFeedbackReleased: Boolean
)

object QuestionResponseDto:

  /** Instantiates a QuestionResponseDto for the given {{questionResponse}} using the given {{distractorOrder}}. This
    * method will return [[Failure]] if the distractor order does not align with the response's selection.
    *
    * @param questionResponse
    *   the response to build a DTO for
    * @param distractorOrder
    *   the distractor order associated with this response
    * @return
    *   [[Failure]] if the selection in questionResponse does not align with the distractor order; otherwise, an
    *   unfiltered question response REST object
    */
  def of(questionResponse: QuizQuestionResponse, distractorOrder: DistractorOrder): Try[QuestionResponseDto] =
    questionResponse.selection.map(distractorOrder.toDisplayOrder) match
      case Some(Success(distractorOrderedSelection))        =>
        Success(
          QuestionResponseDto(
            Some(distractorOrderedSelection),
            questionResponse.attachments,
            questionResponse.state,
            questionResponse.score,
            questionResponse.scorer,
            questionResponse.selectionTime,
            questionResponse.scoreTime,
            questionResponse.instructorFeedback,
            questionResponse.instructorFeedbackReleased
          )
        )
      case Some(failure: Failure[DisplayResponseSelection]) => Failure(failure.exception)
      case None                                             =>
        Success(
          QuestionResponseDto(
            None,
            questionResponse.attachments,
            questionResponse.state,
            questionResponse.score,
            questionResponse.scorer,
            questionResponse.selectionTime,
            questionResponse.scoreTime,
            questionResponse.instructorFeedback,
            questionResponse.instructorFeedbackReleased
          )
        )
end QuestionResponseDto
