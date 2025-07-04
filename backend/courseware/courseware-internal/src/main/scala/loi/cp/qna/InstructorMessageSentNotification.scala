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

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.qna.QnaQuestionFinder
import loi.cp.course.CourseEnrollmentService
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation}
import scalaz.syntax.std.option.*

@Schema("instructorMessageSentNotification")
trait InstructorMessageSentNotification extends Notification:
  type Init = InstructorMessageSentNotificationInit

  @JsonProperty def questionId: Long
  @JsonProperty def messageId: Long
  @JsonProperty def subject: String
  @JsonProperty def body: String
  @JsonProperty def attachments: List[QnaAttachment]

@Component
class InstructorMessageSentNotificationImpl(
  val componentInstance: ComponentInstance,
  val self: NotificationFacade,
)(implicit
  itemService: ItemService,
  courseEnrollmentService: CourseEnrollmentService,
) extends InstructorMessageSentNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(init: InstructorMessageSentNotificationInit): Unit =
    self.setTime(init.question.created)
    self.setSender(init.question.creator.getId.toLong.some)
    self.setContext(init.question.section.getId.toLong.some)
    self.setData(InstructorMessageSentNotificationData(init.question.id))

  override def questionId: Long                 = data.questionId
  override def messageId: Long                  = question.thread.getId
  override def subject: String                  = question.subject
  override def body: String                     = question.thread.message
  override def attachments: List[QnaAttachment] =
    question.thread.attachments.toList.finders[AttachmentFinder].map(QnaAttachment.apply)
  override def aggregationKey: Option[String]   = s"$schemaName:$questionId".some
  override def audience: Iterable[Long]         =
    if question.recipients.isEmpty then courseEnrollmentService.getEnrolledStudentIds(question.section.getId)
    else question.recipients.map(Long2long)

  private lazy val data     = self.getData(classOf[InstructorMessageSentNotificationData])
  private lazy val question = data.questionId.finder[QnaQuestionFinder]
end InstructorMessageSentNotificationImpl

final case class InstructorMessageSentNotificationInit(
  question: QnaQuestionFinder
)

final case class InstructorMessageSentNotificationData(
  questionId: Long
)
