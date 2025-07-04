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

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.{ComponentService, ComponentSupport}
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.util.EntityContext
import com.learningobjects.cpxp.util.EntityContextOps.*

// TODO: Add support for synchronous delivery for the intended audience but asynchronous for observers?

@Service
class NotificationServiceImpl(
  ec: => EntityContext
)(implicit dws: DomainWebService, fs: FacadeService, ns: NotifyService, nw: NotificationWorker, cs: ComponentService)
    extends NotificationService:
  import NotificationServiceImpl.*

  override def notify(parent: Id, notificationCls: Class[? <: Notification], init: Any): Unit =
    val supported = ComponentSupport.hasComponent(notificationCls)
    logger info s"Schedule notification: ${parent.getId}, ${notificationCls.getSimpleName}, supported: $supported"
    if supported then
      val notification = parent.addComponent[Notification](notificationCls, init)
      ec afterCommit {
        nw.offer(dws.getItemRoot(parent.getId), notification.getId)
      }

  override def notifyImmediate(parent: Id, notificationCls: Class[? <: Notification], init: Any): Unit =
    if ComponentSupport.hasComponent(notificationCls) then
      val notification = parent.addComponent[Notification](notificationCls, init)
      ns.scatterEvent(notification)

  override def notification(id: Long): Option[Notification] =
    id.tryComponent[Notification]
end NotificationServiceImpl

object NotificationServiceImpl:
  private final val logger = org.log4s.getLogger
