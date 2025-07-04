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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.notification.SubscribeFinder
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId
import loi.cp.notification.Subscribe.*
import scalaz.std.option.*
import scaloi.misc.JEnumEnum.*
import scaloi.syntax.AnyOps.*
import scaloi.syntax.OptionOps.*

@Service
class SubscriptionServiceImpl(qs: QueryService)(implicit fs: FacadeService) extends SubscriptionService:
  override def subscribe(usr: UserId, crs: ContextId, path: SubscriptionPath, interest: Interest): Unit =
    val notifications = usr.facade[NotificationParentFacade]
    val subscribe     = notifications.getOrCreateSubscribe(crs.id)
    val subscriptions = subscribe.getSubscriptions.getOrElse(Map.empty)
    subscribe.setSubscriptions(subscriptions + (path -> Subscribe(interest)))

  override def unsubscribe(usr: UserId, crs: ContextId, path: SubscriptionPath): Unit =
    for
      subscribe     <- usr.facade[NotificationParentFacade].findSubscribe(crs.id)
      subscriptions <- subscribe.getSubscriptions
    do subscribe.setSubscriptions(subscriptions.view.filterKeys(sub => !path.includes(sub)).toMap)

  override def unsubscribe(usr: UserId, crs: ContextId): Unit =
    val notifications = usr.facade[NotificationParentFacade]
    notifications.findSubscribe(crs.id) foreach { subscribe =>
      subscribe.delete()
    }

  override def subscriptions(usr: UserId, crs: ContextId): Map[SubscriptionPath, Interest] =
    val notifications = usr.facade[NotificationParentFacade]
    notifications
      .findSubscribe(crs.id)
      .flatMap(_.getSubscriptions)
      .getOrElse(Map.empty)
      .view
      .mapValues(_.interest)
      .toMap

  override def interest(usr: UserId, crs: ContextId, path: SubscriptionPath): Option[Interest] =
    for
      subscribe     <- usr.facade[NotificationParentFacade].findSubscribe(crs.id)
      subscriptions <- subscribe.getSubscriptions
      interest      <- interest(subscriptions, path)
    yield interest

  override def findSubscribers(crs: ContextId, path: SubscriptionPath): Map[Long, Interest] =
    querySubscribes(crs.id)
      .getFacades[SubscribeFacade]
      .flatMap(subscribe => subscribe.getParentId.longValue -*> interest(subscribe, path))
      .toMap

  private def interest(subscribe: SubscribeFacade, path: SubscriptionPath): Option[Interest] =
    interest(subscribe.getSubscriptions.getOrElse(Map.empty), path)

  private def interest(
    subscriptions: Map[SubscriptionPath, Subscribe],
    path: SubscriptionPath
  ): Option[Interest] =
    path.ancestry.foldLeft(Option.empty[Interest]) { // this is a foldmap but .... such imports
      case (interest, ancestor) => interest `max` subscriptions.get(ancestor).map(_.interest)
    }

  private def querySubscribes(context: Long): QueryBuilder =
    qs.queryAllDomains(SubscribeFinder.ITEM_TYPE_SUBSCRIBE)
      .addCondition(SubscribeFinder.DATA_TYPE_SUBSCRIBE_CONTEXT, Comparison.eq, context)
end SubscriptionServiceImpl
