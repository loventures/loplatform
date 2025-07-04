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

import loi.cp.assessment.BasicScore
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.actions.AttemptResponseUtils.validateResponseIndex
import loi.cp.quiz.attempt.event.QuizAttemptEvent
import loi.cp.quiz.question.Question
import scalaz.syntax.bifunctor.*
import scalaz.syntax.either.*
import scalaz.{\/, \/-}

case class ConditionallyScoreResponse(questionIndex: Int) extends ConditionalAttemptAction[ScoreResponse]:
  override def condition(params: QuizActionParameters): Boolean =
    // Can I score this?
    scoreAction(params) match
      case \/-(Some(_)) => true // Both can parse everything (from the Right) and has a score (from Some)
      case _            => false

  override def action(params: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    scoreAction(params).widenl.flatMap(possibleAction =>
      // The `condition` function guards against this breaking
      val action: SubmitResponseScore =
        possibleAction.getOrElse(throw new IllegalStateException(s"Events called on $this with a failing condition."))
      action.exec(params)
    )

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] =
    scoreAction(params)
      .map {
        case Some(action) => action.events(params)
        case None         => Nil
      }
      .getOrElse(throw new IllegalStateException(s"Events called on $this with a failing condition."))

  private def scoreAction(params: QuizActionParameters): QuizAttemptSelectionFailure \/ Option[SubmitResponseScore] =
    for
      _             <- validateResponseIndex(params.attemptQuestions, questionIndex).widen[QuizAttemptSelectionFailure, Unit]
      possibleScore <- determineScore(params.attempt, params.attemptQuestions, questionIndex)
    yield possibleScore.map(score => SubmitResponseScore(questionIndex, score, None))

  private def determineScore(
    attempt: QuizAttempt,
    questions: Seq[Question],
    questionIndex: Int
  ): QuizAttemptSelectionFailure \/ Option[BasicScore] =
    val question: Question             = questions(questionIndex)
    val response: QuizQuestionResponse = attempt.responses(questionIndex)

    if !response.state.open then
      AttemptResponseUtils.scoreResponse(
        question,
        response.selection
      )
    else
      // Successful no-op.  Requires a submitted response to score.
      Option.empty[BasicScore].right
  end determineScore

  override def targetClass: Class[ScoreResponse] = classOf[ScoreResponse]
end ConditionallyScoreResponse
