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

import loi.cp.quiz.attempt.DistractorOrder.AuthoredResponseSelection
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.event.{QuizAttemptEvent, SaveResponseEvent}
import scalaz.\/
import scaloi.syntax.boolean.*

import java.time.Instant

/** Updates a selection for a single open [[QuizQuestionResponse]]. This clears any submit state for the response.
  *
  * @param questionIndex
  *   the index of the response to update
  * @param selection
  *   the selection for the response
  */
case class SelectResponse(questionIndex: Int, selection: Option[AuthoredResponseSelection]) extends QuizAttemptAction:
  import AttemptResponseUtils.*

  override def exec(params: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    val attempt: QuizAttempt = params.attempt

    for
      _             <- attempt.valid.elseLeft(InvalidatedAttemptModification).widenl
      _             <- validateResponseIndex(params.attemptQuestions, questionIndex).widenl
      _             <- validateResponseIsOpen(attempt.responses(questionIndex)).widenl
      _             <-
        validateResponseNavigationState(params.quiz.settings.navigationPolicy, attempt.responses, questionIndex).widenl
      question       = params.attemptQuestions(questionIndex)
      _             <- validateSelection(question, selection).widenl
      updatedAttempt = updateSelection(attempt, questionIndex, selection, params.time)
    yield updatedAttempt
    end for
  end exec

  /** Returns a copy of {{attempt}} with the values for the response at {{questionIndex}} updated accordingly.
    *
    * @param attempt
    *   the original attempt
    * @param questionIndex
    *   the index of the response to update
    * @param selection
    *   the new selection
    * @param responseTime
    *   the time for this update
    * @return
    *   a copy of {{attempt} with the given update
    */
  private def updateSelection(
    attempt: QuizAttempt,
    questionIndex: Int,
    selection: Option[AuthoredResponseSelection],
    responseTime: Instant
  ): QuizAttempt =
    val withUpdatedResponse: QuizAttempt =
      updateResponse(attempt, questionIndex) { response =>
        response.copy(
          selection = selection,
          selectionTime = Some(responseTime),
          state = QuestionResponseState.NotSubmitted,
          score = None,
          scorer = None,
          scoreTime = None
        )
      }

    withUpdatedResponse.copy(updateTime = responseTime)
  end updateSelection

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] =
    Seq(SaveResponseEvent(questionIndex, selection))
end SelectResponse
