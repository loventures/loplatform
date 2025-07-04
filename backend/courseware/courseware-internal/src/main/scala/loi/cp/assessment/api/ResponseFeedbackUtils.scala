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

import com.learningobjects.cpxp.service.user.UserId
import loi.cp.assessment.attachment.AssessmentAttachmentService
import loi.cp.assessment.attempt.AssessmentAttempt
import loi.cp.attachment.AttachmentId
import loi.cp.security.SecuritySettings
import scalaz.ValidationNel
import scalaz.std.list.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*

object ResponseFeedbackUtils:
  // returns invalid filenames if there's a problem. add a dsl if there are more failures
  def toQuestionResponseFeedback[T <: AssessmentAttempt](
    attempt: T,
    feedbackRequest: FeedbackRequest,
    now: Instant,
    attachmentService: AssessmentAttachmentService,
    security: SecuritySettings
  ): ValidationNel[String, Feedback] =
    feedbackRequest match
      case BasicFeedbackRequest(comment, feedbackTime, uploadGuids, attachments) =>
        uploadGuids.map(attachmentService.getUpload).traverse(SecuritySettings.validateUpload(security)) map {
          uploads =>
            val newAttachmentIds: Seq[AttachmentId] =
              uploads.map(attachmentService.addAttachment(UserId(attempt.user.id), attempt.id, _))

            val existingAttachments: Seq[AttachmentId] =
              attachments.map(AttachmentId.apply)

            val attachmentIds: Seq[AttachmentId] = existingAttachments ++ newAttachmentIds
            BasicFeedback(comment, feedbackTime | now, attachmentIds)
        }

      case RubricSectionFeedbackRequest(sectionName, comment, feedbackTime, uploadGuids, attachments) =>
        uploadGuids.map(attachmentService.getUpload).traverse(SecuritySettings.validateUpload(security)) map {
          uploads =>
            val newAttachmentIds: Seq[AttachmentId] =
              uploads.map(attachmentService.addAttachment(UserId(attempt.user.id), attempt.id, _))

            val existingAttachments: Seq[AttachmentId] =
              attachments.map(AttachmentId.apply)

            val attachmentIds: Seq[AttachmentId] = existingAttachments ++ newAttachmentIds
            RubricSectionFeedback(sectionName, comment, feedbackTime | now, attachmentIds)
        }
end ResponseFeedbackUtils
