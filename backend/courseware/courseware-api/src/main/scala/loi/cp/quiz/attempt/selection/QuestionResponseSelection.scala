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

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonTypeInfo}

/** A base trait for any type representing a user's selection on a question response. (Question response encompasses the
  * selection, score, feedback etc)
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "responseType")
trait QuestionResponseSelection:
  type Self >: this.type <: QuestionResponseSelection

  // Jackson hates polymorphism, so this is the only way to get a Seq[QuestionResponseSelection] to de-serialize
  @JsonProperty
  def responseType(): String = getClass.getName

  /** Returns whether this selection is indeed just an intentional skipping of the question.
    *
    * @return
    *   whether this selection is indeed just an intentional skipping of the question
    */
  @Deprecated
  @JsonProperty
  def skip: Boolean

  /** Returns the learner's confidence in this selection.
    *
    * @return
    *   the learner's confidence in this selection
    */
  @JsonProperty
  def confidence: Option[Double]

  /** Returns how long the client judged to make this selection.
    *
    * @return
    *   how long the client judged to make this selection
    */
  @JsonProperty
  def timeElapsed: Option[Long]

  /** Returns a sanitized version of this selection in regard to cross site scripting.
    *
    * @return
    *   a sanitized version of this selection in regard to cross site scripting
    */
  @JsonIgnore
  def sanitizedSelection: Self = this
end QuestionResponseSelection
