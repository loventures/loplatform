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

import loi.cp.assessment.ResponseScore
import loi.cp.assessment.attempt.AttemptState
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.event.{AttemptFinalizedEvent, QuizAttemptEvent}
import scalaz.\/
import scalaz.syntax.either.*

case class ScoreAttempt() extends QuizAttemptAction:

  override def exec(params: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    val attempt: QuizAttempt                 = params.attempt
    val responses: Seq[QuizQuestionResponse] = attempt.responses

    val maybeScores: Seq[Option[ResponseScore]] = responses.map(_.score)
    if !attempt.valid then InvalidatedAttemptScoringFailure.left
    else if maybeScores contains None then
      val missingScoreIndices: Seq[Int] = responses.zipWithIndex.filter(_._1.score.isEmpty).map(_._2)
      // There are questions that are not graded yet, so we need more intervention before we can finalize this
      MissingResponseScoresFailure(missingScoreIndices).left
    else
      val updatedResponses: Seq[QuizQuestionResponse] =
        attempt.responses.map(_.copy(state = QuestionResponseState.ResponseScoreReleased))

      val skippedCount: Int    = responses.count(_.selection.isEmpty)
      val quizScore: QuizScore = totalQuizScore(maybeScores.flatten, skippedCount)

      attempt
        .copy(
          score = Some(quizScore),
          state = AttemptState.Finalized,
          scoreTime = Some(params.time),
          updateTime = params.time,
          responses = updatedResponses
        )
        .right
    end if
  end exec

  private def totalQuizScore(scores: Seq[ResponseScore], numberSkipped: Int = 0): QuizScore =
    scores
      .foldLeft(QuizScore.blank)({ case (attemptScore, responseScore) =>
        val isCorrect: Boolean = responseScore.isCorrect
        attemptScore.copy(
          pointsAwarded = attemptScore.pointsAwarded + responseScore.pointsAwarded,
          pointsPossible = attemptScore.pointsPossible + responseScore.pointsPossible,
          itemsCorrect = if isCorrect then attemptScore.itemsCorrect + 1 else attemptScore.itemsCorrect,
          itemsIncorrect = if !isCorrect then attemptScore.itemsIncorrect + 1 else attemptScore.itemsIncorrect
        )
      })
      .copy(itemsSkipped = numberSkipped)

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] = Seq(AttemptFinalizedEvent(false))
end ScoreAttempt
