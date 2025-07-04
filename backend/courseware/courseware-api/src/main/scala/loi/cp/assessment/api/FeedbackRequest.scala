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
package api

import java.time.Instant
import java.util.UUID

import argonaut.*
import com.learningobjects.cpxp.controller.upload.UploadedFile
import scaloi.json.*

/** A request object for an individual piece of feedback.
  */
sealed trait FeedbackRequest extends Product with Serializable

object FeedbackRequest:
  final val FeedbackType = "feedbackType"

  implicit val codec: CodecJson[FeedbackRequest] =
    import ArgoExtras.*
    import Derivation.*

    val basicE  = EncodeJson.derive[BasicFeedbackRequest]
    val basicD  = DecodeJson.derive[BasicFeedbackRequest]
    val rubricE = EncodeJson.derive[RubricSectionFeedbackRequest]
    val rubricD = DecodeJson.derive[RubricSectionFeedbackRequest]

    val encode = sumEncode[FeedbackRequest](FeedbackType) {
      case b: BasicFeedbackRequest         => (Feedback.Basic, basicE.encode(b))
      case r: RubricSectionFeedbackRequest => (Feedback.Rubric, rubricE.encode(r))
    }
    val decode = sumDecode[FeedbackRequest](FeedbackType) {
      case Feedback.Basic  => basicD
      case Feedback.Rubric => rubricD
    }

    CodecJson.derived(using encode, decode)
  end codec
end FeedbackRequest

final case class BasicFeedbackRequest(
  comment: String,
  feedbackTime: Option[Instant] = None,
  uploads: List[UploadedFile] = Nil,
  attachments: List[Long] = Nil,
) extends FeedbackRequest

final case class RubricSectionFeedbackRequest(
  sectionName: UUID,
  comment: String,
  feedbackTime: Option[Instant] = None,
  uploads: List[UploadedFile] = Nil,
  attachments: List[Long] = Nil,
) extends FeedbackRequest
