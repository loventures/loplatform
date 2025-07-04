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

package loi.cp.content
package gate

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId as CourseId
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation}
import loi.cp.reference.EdgePath
import scaloi.misc.TimeSource

@Schema(ContentGateNotification.Schema)
trait ContentGateNotification extends Notification:
  type Init = ContentGateNotification.Init

  @JsonProperty
  def getContentId: EdgePath

  @JsonProperty
  def getTitle: String

  @JsonProperty
  def student: UserId
end ContentGateNotification

object ContentGateNotification:
  // sync with gatingListener.js
  final val Schema = "contentGateNotification"

  final case class Data(content: EdgePath, title: String, student: UserId)
  final case class Init(course: CourseId, user: UserId, content: CourseContent)

@Component
class ContentGateNotificationImpl(
  val componentInstance: ComponentInstance,
  val self: NotificationFacade,
)(
  now: TimeSource,
) extends ContentGateNotification
    with ComponentImplementation
    with NotificationImplementation:
  import ContentGateNotification.*

  @PostCreate def init(init: Init): Unit =
    self.setContext(Some(init.course.id))
    self.setData(Data(init.content.edgePath, init.content.title, init.user))
    self.setTopic(Some(init.content.edgePath.toString))
    self.setTime(now.date)

  override def audience: List[Long] = student.id :: Nil

  override def aggregationKey: Option[String] = Some(s"${`type`}:$getContentId")

  lazy val Data(getContentId, getTitle, student) = self.getData[Data]
end ContentGateNotificationImpl
