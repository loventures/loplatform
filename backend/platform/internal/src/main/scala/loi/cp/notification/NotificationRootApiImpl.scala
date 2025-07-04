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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueries.ApiQueryOps
import com.learningobjects.cpxp.component.query.ApiQuery.Builder
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import scaloi.syntax.AnyOps.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.notification.NotificationConstants.*
import com.learningobjects.cpxp.service.notification.NotifyFinder
import com.learningobjects.cpxp.service.query.{BaseOrder, Comparison, Direction}
import com.learningobjects.cpxp.service.user.UserDTO

@Component
class NotificationRootApiImpl(
  val componentInstance: ComponentInstance,
  user: UserDTO,
  implicit val fs: FacadeService
) extends NotificationRootApi
    with ComponentImplementation:

  /* Check first that we were notified about this notification, as crude access control. */
  override def get(id: Long): Option[Notification] =
    notificationParent.queryNotifies
      .addCondition(NotifyFinder.DATA_TYPE_NOTIFY_NOTIFICATION, Comparison.eq, id)
      .setDataProjection(NotifyFinder.DATA_TYPE_NOTIFY_NOTIFICATION)
      .setLimit(1)
      .getComponent[Notification]

  // Get filtered notifications. currently hardcoded to order by date descending, no filtering
  override def get(q: ApiQuery): ApiQueryResults[Notification] =
    val notificationQuery: ApiQuery =
      new Builder(q).setPage(BaseApiPage.DEFAULT_PAGE).build.withPropertyMappings[Notification]
    val notifyPageQuery: ApiQuery   = new Builder().setPage(q.getPage).build()
    ApiQueries.query[Notification](queryNotifications(notificationQuery), notifyPageQuery)

  private def queryNotifications(notificationQuery: ApiQuery) =
    notificationParent.queryNotifies
      .addOrder(BaseOrder.byData(NotifyFinder.DATA_TYPE_NOTIFY_TIME, Direction.DESC))
      .setDataProjection(NotifyFinder.DATA_TYPE_NOTIFY_NOTIFICATION) <| { qb =>
      // force a join against Notification to ensure that deleted notifications are not counted
      ApiQuerySupport.getQueryBuilder(
        qb.getOrCreateJoinQuery(NotifyFinder.DATA_TYPE_NOTIFY_NOTIFICATION, ITEM_TYPE_NOTIFICATION),
        notificationQuery
      )
    }

  // Get the notification parent for the current user
  private def notificationParent: NotificationParentFacade =
    user.facade[NotificationParentFacade]
end NotificationRootApiImpl
