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

package loi.cp.quiz.attempt.actions

import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.event.QuizAttemptEvent
import scalaz.\/
import scaloi.syntax.boolean.*

import java.time.Instant

/** Submits a response, closing it.
  *
  * @param questionIndex
  *   the index of the response to update
  */
case class SubmitResponse(questionIndex: Int) extends QuizAttemptAction:
  import AttemptResponseUtils.*

  override def exec(params: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    val attempt: QuizAttempt = params.attempt

    for
      _             <- attempt.valid.elseLeft(InvalidatedAttemptModification).widenl
      _             <- validateResponseIndex(params.attemptQuestions, questionIndex).widenl
      _             <- validateResponseIsOpen(attempt.responses(questionIndex)).widenl
      _             <-
        validateResponseNavigationState(params.quiz.settings.navigationPolicy, attempt.responses, questionIndex).widenl
      updatedAttempt = updateResponseValue(attempt, questionIndex, params.time)
    yield updatedAttempt
  end exec

  private def updateResponseValue(attempt: QuizAttempt, questionIndex: Int, submitTime: Instant): QuizAttempt =
    val withUpdatedResponse: QuizAttempt =
      updateResponse(attempt, questionIndex) { response =>
        // If we do not have a selection, then we are implicitly selecting [[None]] right now.  Thus, this submission
        // time becomes the selection time if we did not have a selection time.
        val selectionTime: Option[Instant] =
          if response.selectionTime.nonEmpty then response.selectionTime
          else Some(submitTime)

        response.copy(state = QuestionResponseState.ResponseSubmitted, selectionTime = selectionTime)
      }

    withUpdatedResponse.copy(updateTime = submitTime)
  end updateResponseValue

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] = Nil
end SubmitResponse
