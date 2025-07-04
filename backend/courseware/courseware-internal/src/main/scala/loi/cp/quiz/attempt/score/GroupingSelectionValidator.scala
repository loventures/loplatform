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

import loi.cp.quiz.attempt.selection.GroupingSelection
import loi.cp.quiz.attempt.{InvalidSelectionIndex, QuizAttemptSelectionFailure, TooManySelections}
import scalaz.\/
import scalaz.syntax.either.*

object GroupingSelectionValidator:
  private[score] def validate(
    selection: GroupingSelection,
    maxAllowedTermIndex: Int,
    maxAllowedDefIndex: Int,
    allowMultipleDefsPerTerm: Boolean = true
  ): QuizAttemptSelectionFailure \/ GroupingSelection =
    if selection.elementIndexesByGroupIndex.isEmpty then selection.right
    else
      val choices: Map[Int, Seq[Int]] = selection.elementIndexesByGroupIndex

      val terms                = choices.keys
      val definitions          = choices.values.flatten
      val maxDefsForSingleTerm = choices.values.map(_.size).max

      if terms.max >= maxAllowedTermIndex
        || definitions.max >= maxAllowedDefIndex
        || terms.min < 0
        || definitions.min < 0
      then InvalidSelectionIndex.left[GroupingSelection]
      else if !allowMultipleDefsPerTerm && maxDefsForSingleTerm > 1 then
        TooManySelections(1, maxDefsForSingleTerm).left[GroupingSelection]
      else selection.right
end GroupingSelectionValidator
