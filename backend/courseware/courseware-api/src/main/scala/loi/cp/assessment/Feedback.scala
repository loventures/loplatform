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

package loi.cp.assessment

import java.time.Instant
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import loi.cp.attachment.AttachmentId

/** An object representing instructor feedback.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "feedbackType")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[BasicFeedback], name = Feedback.Basic),
    new JsonSubTypes.Type(value = classOf[RubricSectionFeedback], name = Feedback.Rubric)
  )
)
sealed trait Feedback:
  val feedbackType: String
  val comment: String
  val feedbackTime: Instant
  val attachments: Seq[AttachmentId]
end Feedback

object Feedback:
  final val Basic  = "basic"
  final val Rubric = "rubric"

case class BasicFeedback(comment: String, feedbackTime: Instant, attachments: Seq[AttachmentId]) extends Feedback:
  override val feedbackType: String = Feedback.Basic

case class RubricSectionFeedback(
  sectionName: UUID,
  comment: String,
  feedbackTime: Instant,
  attachments: Seq[AttachmentId]
) extends Feedback:
  override val feedbackType: String = Feedback.Rubric
