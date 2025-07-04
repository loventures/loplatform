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

import loi.cp.quiz.attempt.selection.ChoiceSelection
import loi.cp.quiz.attempt.{InvalidSelectionIndex, QuizAttemptSelectionFailure, TooManySelections}
import scalaz.\/
import scalaz.syntax.either.*

object ChoiceSelectionValidators:
  private[score] def validate(
    selection: ChoiceSelection,
    allowedSelectionCount: Int,
    numChoices: Int
  ): QuizAttemptSelectionFailure \/ ChoiceSelection =
    if selection.selectedIndexes.size > allowedSelectionCount then
      TooManySelections(allowedSelectionCount, selection.selectedIndexes.size).left[ChoiceSelection]
    else if selection.selectedIndexes.nonEmpty && (selection.selectedIndexes.max >= numChoices || selection.selectedIndexes.min < 0)
    then InvalidSelectionIndex.left[ChoiceSelection]
    else selection.right
end ChoiceSelectionValidators
