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

package loi.cp.quiz.attempt

import loi.cp.assessment.Score

/** A score for a quiz attempt.
  *
  * @param pointsAwarded
  *   the number of points the user got
  * @param pointsPossible
  *   the total number of points the user could have possibly obtained for the attempt
  * @param itemsCorrect
  *   the number of questions the user responded correctly to (obtained all points for)
  * @param itemsIncorrect
  *   the number of questions the user did not respond correctly to (obtained less than all points for)
  * @param itemsSkipped
  *   the number of questions the user skipped
  */
case class QuizScore(
  pointsAwarded: Double,
  pointsPossible: Double,
  itemsCorrect: Long,
  itemsIncorrect: Long,
  itemsSkipped: Long
) extends Score:
  override def equals(that: Any): Boolean =
    that match
      case QuizScore(thatPointsAwarded, thatPointsPossible, thatItemsCorrect, thatItemsIncorrect, thatItemsSkipped) =>
        (Math.abs(thatPointsAwarded - pointsAwarded) < Score.Epsilon) &&
        (Math.abs(thatPointsPossible - pointsPossible) < Score.Epsilon) &&
        thatItemsCorrect == itemsCorrect &&
        thatItemsIncorrect == itemsIncorrect &&
        thatItemsSkipped == itemsSkipped
      case _                                                                                                        => false
end QuizScore

object QuizScore:
  val blank: QuizScore = QuizScore(0.0, 0.0, 0L, 0L, 0L)
