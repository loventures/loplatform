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

package loi.cp.quiz.attempt.score

import loi.asset.question.QuestionScoringOption
import loi.cp.assessment.BasicScore
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.selection.ChoiceSelection
import loi.cp.quiz.question.choice.{Choice, ChoiceQuestion}

object ChoiceResponseScorer:
  def score(
    selection: ChoiceSelection,
    question: ChoiceQuestion,
    scoringOption: QuestionScoringOption = QuestionScoringOption.AllOrNothing
  ): BasicScore =
    val selectedChoices: Seq[Choice] = selection.selectedIndexes.map(idx => question.choices(idx))
    val pointsAwarded: Double        = selectedChoices.map(_.points).sum
    val anyCorrect: Boolean          = selectedChoices.exists(_.correct)
    val maxAllocatedPossible: Double = question.choices.filter(_.correct).foldLeft(0.0)(_ + _.points)
    val percentageAwarded: Double    = pointsAwarded / maxAllocatedPossible
    val actualPointsAwarded: Double  = percentageAwarded * question.pointValue
    scoringOption match
      case QuestionScoringOption.AllOrNothing                  =>
        ResponseScores.allOrNothingOf(actualPointsAwarded, question)
      case QuestionScoringOption.AllowPartialCredit            =>
        ResponseScores.of(actualPointsAwarded, question)
      case QuestionScoringOption.FullCreditForAnyCorrectChoice =>
        if anyCorrect then ResponseScores.allOf(question) else ResponseScores.zero(question)
  end score
end ChoiceResponseScorer
