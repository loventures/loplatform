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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import loi.cp.assessment.{Feedback, ResponseScore}
import loi.cp.attachment.AttachmentId
import loi.cp.quiz.attempt.DistractorOrder.AuthoredResponseSelection

/** A student's response, with potential feedback for a question.
  *
  * @param selection
  *   the selection the user made, if any
  * @param attachments
  *   the attachments for the response (this should always be an empty list for anything other than essays)
  * @param state
  *   the current state of the response
  * @param score
  *   the current score for the response, if any (the semantic meaning of this value should only be derived from the
  *   {{state}})
  * @param scorer
  *   the id of the user who issued {{score}}; or [[None]] if the system scored this response
  * @param selectionTime
  *   when the selection was made, if this response was submitted without a selection ever being made, then this will be
  *   the submission time
  * @param scoreTime
  *   when the score was last updated
  * @param instructorFeedback
  *   any feedback the instructor has provided on this response
  * @param instructorFeedbackReleased
  *   whether the user should see the {{instructorFeedback}}
  */
case class QuizQuestionResponse(
  selection: Option[AuthoredResponseSelection],
  attachments: Seq[AttachmentId],
  state: QuestionResponseState,
  score: Option[ResponseScore],
  @JsonDeserialize(contentAs = classOf[JLong]) scorer: Option[Long],
  selectionTime: Option[Instant],
  scoreTime: Option[Instant],
  instructorFeedback: Seq[Feedback],
  instructorFeedbackReleased: Boolean
)

object QuizQuestionResponse:
  def empty: QuizQuestionResponse =
    QuizQuestionResponse(None, Nil, QuestionResponseState.NotSubmitted, None, None, None, None, Nil, false)
