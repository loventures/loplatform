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

/** A selection representing a response to one or more blank entries.
  */
case class BlankEntriesSelection(
  skip: Boolean,
  @JsonDeserialize(contentAs = classOf[JDouble]) confidence: Option[Double],
  @JsonDeserialize(contentAs = classOf[JLong]) timeElapsed: Option[Long],
  entries: Seq[String]
) extends QuestionResponseSelection:
  override type Self = BlankEntriesSelection

  override def sanitizedSelection: BlankEntriesSelection = this.copy(entries = this.entries.map(ComponentUtils.dexss))

object BlankEntriesSelection:
  def apply(entries: Seq[String]): BlankEntriesSelection = BlankEntriesSelection(false, None, None, entries)
