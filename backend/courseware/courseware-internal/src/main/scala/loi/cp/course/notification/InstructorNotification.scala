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

package loi.cp.course.notification

import java.time.Instant
import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.context.ContextId
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation, NotificationUrgency}
import loi.cp.reference.EdgePath

/** A notification to a user from an instructor
  */
@Schema("instructorNotification")
trait InstructorNotification extends Notification:
  @JsonProperty
  def message: String

@Component
class InstructorNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)
    extends InstructorNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(init: InstructorNotification.Init): Unit =
    self.setTime(Date.from(init.time))
    self.setSender(Some(init.senderId))
    self.setContext(Some(init.contextId.value))
    self.setTopic(Option(init.edgePath.toString))
    self.setData[InstructorNotification.Dto](InstructorNotification.toDto(init))

  override lazy val aggregationKey: Option[String] = Some(s"$schemaName:${data.edgePath}")

  override def bindings(implicit domain: DomainDTO, user: UserDTO, cs: ComponentService): Map[String, Any] =
    super.bindings(using domain, user, cs) ++ Some("message" -> message)

  override lazy val urgency: NotificationUrgency = data.urgency

  override lazy val audience: Seq[Long] = data.notifiedUserIds

  override lazy val message: String = data.message

  private lazy val data: InstructorNotification.Dto =
    self.getData(classOf[InstructorNotification.Dto])
end InstructorNotificationImpl

object InstructorNotification:

  case class Init(
    edgePath: EdgePath,
    contextId: ContextId,
    time: Instant,
    senderId: Long,
    message: String,
    notifiedUserIds: Seq[Long],
    urgency: NotificationUrgency
  )

  case class Dto(
    message: String,
    @JsonDeserialize(contentAs = classOf[JLong]) notifiedUserIds: Seq[Long],
    urgency: NotificationUrgency,
    edgePath: EdgePath
  )

  def toDto(init: Init): Dto =
    Dto(init.message, init.notifiedUserIds, init.urgency, init.edgePath)
end InstructorNotification
