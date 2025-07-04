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

package loi.cp.quiz.attempt.exceptions

import loi.cp.quiz.attempt.DistractorOrder
import loi.cp.quiz.attempt.selection.QuestionResponseSelection

import scala.util.control.NoStackTrace

/** An exception for when a selection is not congruent with the given distractor order. Either the caller in question
  * passed the wrong value, or the persisted value stored was manipulated outside the normal execution of the system.
  *
  * @param selection
  *   the selection provided
  * @param order
  *   the distractor order provided
  */
case class InvalidSelectionOrderingException(selection: QuestionResponseSelection, order: DistractorOrder)
    extends RuntimeException(s"$order cannot be used for $selection")
    with NoStackTrace
