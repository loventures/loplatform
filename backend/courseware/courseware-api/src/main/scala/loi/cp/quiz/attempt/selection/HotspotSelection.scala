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
import com.learningobjects.cpxp.scala.util.JTypes.{JDouble, JLong}

/** A selection representing a coordinate that a user selected on an image. The coordinates are calculated with the
  * upper left hand corner representing [0, 0].
  */
case class HotspotSelection(
  skip: Boolean,
  @JsonDeserialize(contentAs = classOf[JDouble]) confidence: Option[Double],
  @JsonDeserialize(contentAs = classOf[JLong]) timeElapsed: Option[Long],
  point: Option[Point]
) extends QuestionResponseSelection

final case class Point(x: Double, y: Double)

object HotspotSelection:
  def apply(point: Option[Point]): HotspotSelection = HotspotSelection(true, None, None, point)
