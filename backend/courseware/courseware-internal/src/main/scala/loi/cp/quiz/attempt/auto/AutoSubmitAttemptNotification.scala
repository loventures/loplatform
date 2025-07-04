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

package loi.cp.quiz.attempt.auto

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.AttemptId
import loi.cp.context.ContextId
import loi.cp.notification.*
import loi.cp.reference.EdgePath

import java.time.Instant
import java.util.Date

@Schema("autoSubmitAttemptNotification")
trait AutoSubmitAttemptNotification extends Notification:
  type Init = AutoSubmitAttemptNotification.Init

  @JsonProperty
  def attemptId: AttemptId

  @JsonProperty
  def title: String

@Component
class AutoSubmitAttemptNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)
    extends AutoSubmitAttemptNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(init: AutoSubmitAttemptNotification.Init): Unit =
    self.setTime(Date.from(init.time))
    self.setSender(None)
    self.setContext(Some(init.contextId.value))
    self.setTopic(Option(init.quizId.toString))
    self.setData[AutoSubmitAttemptNotification.Init](init)

  lazy val attemptId: AttemptId = data.attemptId

  lazy val title: String = data.quizTitle

  override lazy val aggregationKey = Some(s"$schemaName:${attemptId.value}")

  override def bindings(implicit domain: DomainDTO, user: UserDTO, cs: ComponentService): Map[String, Any] =
    super.bindings(using domain, user, cs) ++ Some("attemptId" -> attemptId) ++ Some("title" -> title)

  override def urgency = NotificationUrgency.Safe

  override def audience = Seq(data.userId)

  private lazy val data: AutoSubmitAttemptNotification.Init =
    self.getData(classOf[AutoSubmitAttemptNotification.Init])
end AutoSubmitAttemptNotificationImpl

object AutoSubmitAttemptNotification:
  case class Init(
    quizTitle: String,
    quizId: EdgePath,
    attemptId: AttemptId,
    contextId: ContextId,
    userId: Long,
    time: Instant
  )
