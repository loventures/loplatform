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

package loi.cp.quiz.attempt.selection

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.scala.util.JTypes.{JDouble, JInteger, JLong}

/** A selection representing the correct order of the sequence.
  *
  * Example:
  *   - The Great War
  *   - The War of 1812
  *   - World War II
  *   - The Thirty Years War
  *
  * The correct response for this distractor randomization would be: [3, 1, 0, 2]
  */
case class OrderingSelection(
  skip: Boolean,
  @JsonDeserialize(contentAs = classOf[JDouble]) confidence: Option[Double],
  @JsonDeserialize(contentAs = classOf[JLong]) timeElapsed: Option[Long],
  @JsonDeserialize(contentAs = classOf[JInteger]) order: Seq[Int]
) extends QuestionResponseSelection

object OrderingSelection:
  def apply(selectedIndexes: Seq[Int]): OrderingSelection = OrderingSelection(false, None, None, selectedIndexes)
