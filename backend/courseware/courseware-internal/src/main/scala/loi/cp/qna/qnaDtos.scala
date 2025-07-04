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

package loi.cp.qna

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import com.learningobjects.cpxp.service.qna.{QnaMessageFinder, QnaQuestionFinder}
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.{DeXssSupport, FormattingUtils}
import loi.cp.reference.EdgePath
import scalaz.Semigroup

import java.util.Date

private[qna] final case class NewQuestion(
  edgePath: EdgePath,
  html: String,
  attachments: List[UploadInfo],
)

final case class NewMessage(
  html: String,
  attachments: List[UploadInfo],
  category: Option[String] = None,
  subcategory: Option[String] = None,
)

private[qna] final case class Recategorize(
  category: Option[String] = None,
  subcategory: Option[String] = None,
)

private[qna] final case class EditMessage(
  html: String,
  attachments: List[Long],
)

private[qna] final case class NewInstructorMessage(
  subject: String,
  html: String,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  recipients: List[Long] = Nil,
  attachments: List[UploadInfo] = Nil
)

/** @param instructorMessage
  *   true when this is an instructor message that has not been replied to yet, false otherwise
  */
private[qna] final case class QnaQuestion(
  id: Long,
  sectionId: Long,
  edgePath: EdgePath,
  created: Date,
  creator: QnaProfile,
  modified: Date,
  subject: Option[String],
  open: Boolean,
  closed: Boolean,
  messages: List[QnaMessage],
  category: Option[String],
  subcategory: Option[String],
  instructorMessage: Boolean,
  recipients: Option[List[QnaProfile]],
)

private[qna] object QnaQuestion:
  def apply(
    question: QnaQuestionFinder,
    messages: List[QnaMessage],
    recipients: Option[List[QnaProfile]] = None,
  ): QnaQuestion =
    new QnaQuestion(
      id = question.id,
      sectionId = question.section.getId,
      edgePath = EdgePath.parse(question.edgePath),
      created = question.created,
      creator = QnaProfile(question.creator),
      modified = question.modified,
      subject = Option(question.subject),
      open = question.open,
      closed = question.closed,
      messages = messages,
      category = Option(question.category),
      subcategory = Option(question.subcategory),
      instructorMessage = question.recipients ne null,
      recipients = recipients,
    )
end QnaQuestion

private[qna] final case class QnaProfile(
  id: Long,
  fullName: String,
  externalId: String,
)

private[qna] object QnaProfile:
  def apply(user: UserFinder): QnaProfile = new QnaProfile(
    user.id,
    FormattingUtils.userStr(user.userName, user.givenName, user.middleName, user.familyName),
    user.externalId,
  )

private[qna] final case class QnaMessage(
  id: Long,
  created: Date,
  edited: Option[Date],
  creator: QnaProfile,
  html: String,
  attachments: List[QnaAttachment],
)

private[qna] object QnaMessage:
  def apply(message: QnaMessageFinder, attachments: List[QnaAttachment]): QnaMessage =
    new QnaMessage(
      id = message.id,
      created = message.created,
      edited = Option(message.edited),
      creator = QnaProfile(message.creator),
      html = DeXssSupport.deXss(message.message, s"QnaMessage:${message.id}"),
      attachments = attachments,
    )
end QnaMessage

private[qna] final case class QnaAttachment(
  id: Long,
  fileName: String,
  size: Long,
)

private[qna] object QnaAttachment:
  def apply(attachment: AttachmentFinder): QnaAttachment = new QnaAttachment(
    attachment.id,
    attachment.fileName,
    attachment.size,
  )

private[qna] final case class QnaSummary(
  edgePath: EdgePath,
  count: Long,
  open: Long,
  answered: Long,
)

private[qna] object QnaSummary:
  implicit val QnaSummarySemigroup: Semigroup[QnaSummary] = Semigroup.instance((a, b) =>
    a.copy(count = a.count + b.count, open = a.open + b.open, answered = a.answered + b.answered)
  )
