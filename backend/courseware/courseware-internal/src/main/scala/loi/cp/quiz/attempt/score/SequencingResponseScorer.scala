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
import loi.cp.quiz.attempt.exceptions.MismatchedDistractorOrderException
import loi.cp.quiz.attempt.selection.OrderingSelection
import loi.cp.quiz.question.ordering.Sequencing
import scalaz.\/
import scalaz.syntax.either.*

object SequencingResponseScorer:
  def score(
    selection: OrderingSelection,
    question: Sequencing
  ): InvalidSelectionIndex.type \/ BasicScore =
    for validSelection <- validateSelectionValues(selection, question.choices.size)
    yield
      val correctSequence =
        validateDistractorOrder(question, AuthoredOrder.instance) // FIXME hardcoded order, yet again
      if correctSequence.equals(validSelection.order) then ResponseScores.allOf(question)
      else ResponseScores.zero(question)

  private def validateSelectionValues(
    selection: OrderingSelection,
    distractors: Int
  ): InvalidSelectionIndex.type \/ OrderingSelection =
    if selection.order.max >= distractors || selection.order.min < 0 || selection.order.size > distractors then
      InvalidSelectionIndex.left
    else selection.right

  private def validateDistractorOrder(
    question: Sequencing,
    distractorOrder: ChoiceDistractorOrderType
  ): Seq[Int] =
    distractorOrder match
      case DistractorIndexList(responseOrder) =>
        if responseOrder.size != question.choices.size then
          // Something went seriously wrong if we somehow are giving a distractor order that is not aligning with a
          // question
          throw new MismatchedDistractorOrderException(question, distractorOrder)
        else responseOrder
      case AuthoredOrder()                    => question.choices.indices // Identity transformation
end SequencingResponseScorer
