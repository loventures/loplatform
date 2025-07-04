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

package loi.cp.quiz.attempt.notification

import java.time.Instant
import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.AttemptId
import loi.cp.context.ContextId
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation, NotificationUrgency}
import loi.cp.reference.EdgePath

/** A notification to a user that one of their attempts has been invalidated.
  */
@Schema("attemptInvalidatedNotification")
trait AttemptInvalidationNotification extends Notification:
  type Init = AttemptInvalidationNotification.Init

  @JsonProperty
  def attemptId: AttemptId

  @JsonProperty
  def title: String

@Component
class AttemptInvalidationNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)
    extends AttemptInvalidationNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(init: AttemptInvalidationNotification.Init): Unit =
    self.setTime(Date.from(init.time))
    self.setSender(None)
    self.setContext(Some(init.contextId.value))
    self.setTopic(Option(init.quizId.toString))
    self.setData[AttemptInvalidationNotification.Init](init)

  lazy val attemptId: AttemptId = data.attemptId

  lazy val title: String = data.quizTitle

  override lazy val aggregationKey = Some(s"$schemaName:${attemptId.value}")

  override def bindings(implicit domain: DomainDTO, user: UserDTO, cs: ComponentService): Map[String, Any] =
    super.bindings(using domain, user, cs) ++ Some("attemptId" -> attemptId) ++ Some("title" -> title)

  override def urgency = NotificationUrgency.Safe

  override def audience = Seq(data.userId)

  private lazy val data: AttemptInvalidationNotification.Init =
    self.getData(classOf[AttemptInvalidationNotification.Init])
end AttemptInvalidationNotificationImpl

object AttemptInvalidationNotification:
  case class Init(
    quizTitle: String,
    quizId: EdgePath,
    attemptId: AttemptId,
    contextId: ContextId,
    userId: Long,
    time: Instant
  )
