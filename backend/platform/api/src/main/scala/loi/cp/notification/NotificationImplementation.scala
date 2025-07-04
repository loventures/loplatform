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

package loi.cp.notification

import java.lang.Long as JLong
import java.util.Date

import com.learningobjects.cpxp.component.{ComponentService, ComponentSupport}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.context.CourseContextComponent
import loi.cp.email.Email
import loi.cp.user.UserComponent

/** A mixin that provides useful default implementations of most of the notification interface methods.
  */
trait NotificationImplementation:
  selph: Notification =>
  protected val self: NotificationFacade

  override def getId: JLong = self.getId

  override def getSender: Option[Long] = self.getSender

  override def getContext: Option[Long] = self.getContext

  override def getTime: Date = self.getTime

  override def getTopic: Option[String] = self.getTopic

  /** The default subscription path is empty. */
  override def subscriptionPath: Option[SubscriptionPath] = None

  /** The default audience is empty. */
  override def audience: Iterable[Long] = List.empty

  /** The default interest is to alert the intended audience. */
  override def interest: Interest = Interest.Alert

  /** The default aggregation key is empty. */
  override def aggregationKey: Option[String] = None

  /** The default bindings include the domain, the sender, the course, and the notification object. */
  override def bindings(implicit domain: DomainDTO, user: UserDTO, cs: ComponentService): Map[String, Any] =
    Map("domain" -> domain, "notification" -> this, "user" -> user.component[UserComponent]) ++
      getContext.map(c => "course" -> c.component[CourseContextComponent])

  override def urgency: NotificationUrgency = NotificationUrgency.Safe

  override def emailInfo: Option[Notification.EmailInfo[? <: Email]] =
    Some(Notification.EmailInfo(classOf[GenericNotificationEmail], Email.Init(Some(getId.longValue), None)))

  override final def `type`: String = schemaName

  protected def schemaName: String = ComponentSupport.getSchemaName(this)
end NotificationImplementation
