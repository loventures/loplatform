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
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.qna.{QnaMessageFinder, QnaQuestionFinder}
import loi.cp.course.CourseSectionService
import loi.cp.email.Email
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation}
import loi.cp.reference.EdgePath
import scalaz.syntax.std.option.*

@Schema("qnaNotification")
trait QnaNotification extends Notification:
  type Init = QnaNotificationInit

  @JsonProperty
  def questionId: Long
  @JsonProperty
  def messageId: Long
  @JsonProperty
  def edgePath: EdgePath
end QnaNotification

@Component
class QnaNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)(implicit
  courseSectionService: CourseSectionService,
  is: ItemService
) extends QnaNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(init: QnaNotificationInit): Unit =
    self.setTime(init.message.created)
    self.setSender(Some(init.message.creator.getId))
    self.setContext(Some(init.question.section.getId))
    self.setData(QnaNotificationDto(init.message.id))

  override def questionId: Long = self.getParentId

  override def messageId: Long = data.messageId

  override def edgePath: EdgePath = EdgePath.parse(question.edgePath)

  override def aggregationKey: Option[String] = Some(s"$schemaName:$questionId")

  override def audience: Iterable[Long] = Seq(question.creator.getId)

  override def emailInfo: Option[Notification.EmailInfo[QnaNotificationEmail]] =
    Notification.EmailInfo(classOf[QnaNotificationEmail], Email.Init(Some(getId), None)).some

  private lazy val data     = self.getData(classOf[QnaNotificationDto])
  private lazy val question = questionId.finder[QnaQuestionFinder]
end QnaNotificationImpl

private[qna] final case class QnaNotificationInit(question: QnaQuestionFinder, message: QnaMessageFinder)

private[qna] final case class QnaNotificationDto(messageId: Long)
