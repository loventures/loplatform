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
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.scala.util.JTypes.{JDouble, JLong}

/** A selection representing a response to a single free text field.
  */
case class FreeTextSelection(
  skip: Boolean,
  @JsonDeserialize(contentAs = classOf[JDouble]) confidence: Option[Double],
  @JsonDeserialize(contentAs = classOf[JLong]) timeElapsed: Option[Long],
  response: Option[String]
) extends QuestionResponseSelection:
  override type Self = FreeTextSelection

  override def sanitizedSelection: FreeTextSelection =
    this.copy(response = response.map(ComponentUtils.dexss))
end FreeTextSelection

object FreeTextSelection:
  def apply(response: String): FreeTextSelection = FreeTextSelection(true, None, None, Some(response))

  def apply(response: Option[String]): FreeTextSelection = FreeTextSelection(true, None, None, response)
