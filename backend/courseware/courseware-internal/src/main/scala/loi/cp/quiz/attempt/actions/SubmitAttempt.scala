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

import loi.cp.assessment.attempt.AttemptState.Submitted
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.event.{AttemptSubmittedEvent, QuizAttemptEvent}
import scalaz.\/
import scalaz.syntax.either.*

case class SubmitAttempt(autoSubmit: Boolean) extends QuizAttemptAction:

  override def exec(params: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    val unrespondedIndices: List[Int] =
      params.attempt.responses.zipWithIndex.filter(_._1.state.open).map(_._2).toList

    val submitAggregateAttempt =
      AggregateAttemptAction(
        unrespondedIndices.map(index => SubmitResponse(index) `andThen` ConditionallyScoreResponse(index))
      )

    val updatedAttempt: QuizAttemptFailure \/ QuizAttempt = submitAggregateAttempt.exec(params)

    if params.attempt.valid then
      updatedAttempt map { att =>
        att.copy(
          submitTime = Some(params.time),
          autoSubmitted = autoSubmit,
          state = Submitted,
          updateTime = params.time
        )
      }
    else InvalidatedAttemptModification.left
    end if
  end exec

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] = Seq(AttemptSubmittedEvent(false))
end SubmitAttempt
