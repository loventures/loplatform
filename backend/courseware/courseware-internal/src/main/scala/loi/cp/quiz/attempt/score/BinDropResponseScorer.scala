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

import loi.cp.assessment.BasicScore
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.selection.GroupingSelection
import loi.cp.quiz.question.bindrop.{Bin, BinDrop}
import scalaz.\/

object BinDropResponseScorer:
  type BinIndex    = Int
  type OptionIndex = Int

  def score(selection: GroupingSelection, question: BinDrop): QuizAttemptSelectionFailure \/ BasicScore =
    // Based on BinDropQuestionResponseGrader
    // FIXME this can go away because everything's already in authored order
    val order: BinDropDistractorOrder = BinDropDistractorOrder(question.bins.indices, question.options.indices)

    for validatedSelection <- GroupingSelectionValidator.validate(selection, question.bins.size, question.options.size)
    yield
      // Both the correct answer and the selection are in /response/ order at this point
      val correctBins: Seq[Bin] =
        for
          (bin, responseBinIndex) <- order.binsInResponseOrder(question.bins).zipWithIndex
          selectedResponseOptions  = validatedSelection.elementIndexesByGroupIndex.getOrElse(responseBinIndex, Nil)
          if bin.isCorrect(selectedResponseOptions.toSet)
        yield bin

      val binCount: Int = question.bins.size

      val percentCorrect: Double = correctBins.size.toDouble / binCount.toDouble
      val pointsAwarded: Double  = percentCorrect * question.pointValue
      if question.awardsPartialCredit then ResponseScores.of(pointsAwarded, question)
      else ResponseScores.allOrNothingOf(pointsAwarded, question)
    end for
  end score
end BinDropResponseScorer
