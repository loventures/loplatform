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

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.UserWebService
import com.learningobjects.cpxp.util.ManagedUtils
import loi.cp.bus.MessageBusService
import loi.cp.config.ConfigurationService
import loi.cp.context.ContextId
import loi.cp.email.Email
import loi.cp.presence.PresenceService
import loi.cp.reply.ReplyService
import loi.cp.user.{UserComponent, UserPreferences}
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.functor.*
import scaloi.syntax.BooleanOps.*

@Service
class NotifyService(implicit
  cs: ConfigurationService,
  fs: FacadeService,
  mbs: MessageBusService,
  om: ObjectMapper,
  ps: PresenceService,
  qs: QueryService,
  rs: ReplyService,
  ss: SubscriptionService,
  uws: UserWebService,
  cs2: ComponentService
):
  import NotifyService.*

  /** Scatter events, returning (number of users in audience, number of users notified, number of emails sent) */
  def scatterEvent(event: Notification): (Int, Int, Int) =
    // gather the interest of the intended audience, considering any subscriptions they may have,
    // plus always notify but never alert the subject. Filter out all mutes
    val audience                       = event.audience.toList.strengthR(event.interest).toMap
    val watchers                       = subscribers(event).getOrElse(Map.empty)
    val sender                         = event.getSender.strengthR(Interest.Notify)
    val interests: Map[Long, Interest] = (audience ++ watchers ++ sender).filterNot(_._2 == Interest.Mute)
    // get the audience sorted by pk to avoid deadlock
    val users                          = interests.keys.toSeq.sorted.component[UserComponent]

    // These loops run separately so that Hibernate can do a better job of batching updates.
    // The alert get-or-creates inherently flush inserts to the database so run basically
    // one insert per statement... If this is a common occurrence, need to come up with an
    // optimized strategy.
    users foreach { u =>
      notifyUser(u, event)
    }
    val alerted = users filter { u =>
      alertUser(u, interests(u.getId.longValue), event).isDefined
    }
    val emails  = alerted flatMap { u =>
      emailUser(u, event)
    }

    event.facade[NotificationFacade].setProcessed(true)

    ManagedUtils.commit()

    // deliver notify events
    ps.deliverToUsers(notificationEvent(event))(users.map(_.getId.longValue)*)
    // deliver alert events
    ps.deliverToUsers(alertEvent(event))(alerted.map(_.getId.longValue)*)

    (users.size, alerted.size, emails.size)
  end scatterEvent

  private def subscribers(event: Notification): Option[Map[Long, Interest]] =
    for
      ctx  <- event.getContext
      path <- event.subscriptionPath
    yield ss.findSubscribers(ContextId(ctx), path)

  private def notifyUser(user: UserComponent, notification: Notification): NotifyFacade =
    user.facade[NotificationParentFacade] addNotify { notify =>
      notify.setTime(notification.getTime)
      notify.setNotification(notification)
      notify.pollute()
    }

  private def alertUser(user: UserComponent, interest: Interest, notification: Notification): Option[AlertFacade] =
    for
      aggregationKey <- notification.aggregationKey
      if interest == Interest.Alert
    yield user.facade[NotificationParentFacade].getOrCreateAlertByAggregationKeyAndViewed(aggregationKey) init { a =>
      a.setContext(notification.getContext)
      a.setCount(1)
    } update { a =>
      a.refresh(true) // pessimistic lock
      a.setCount(1 + a.getCount)
    } always { a =>
      a.setNotification(notification)
      a.setTime(notification.getTime)
      a.pollute()
    }

  private def emailUser(user: UserComponent, notification: Notification): Option[Email] =
    shouldEmail(user).flatOption(notification.emailInfo) flatMap { info =>
      rs.scheduleEmail(user, info.impl, info.init)
    }

  private def shouldEmail(user: UserComponent): Boolean =
    UserPreferences.getItem(user).sendAlertEmails
end NotifyService

object NotifyService:

  /** Construct a notification SSE event. Drives a user viewing their notifications.
    *
    * @param notification
    *   the notification
    * @param mapper
    *   the object mapper
    * @return
    *   the notify event
    */
  private def notificationEvent(notification: Notification)(implicit mapper: ObjectMapper): NotificationEvent =
    NotificationEvent(mapper.writeValueAsString(notification))

  /** Construct an alert SSE event. Drives an alert bell in the UI.
    *
    * @param notification
    *   the notification
    * @return
    *   the alert event
    */
  private def alertEvent(notification: Notification): AlertEvent =
    AlertEvent(notification.getId, notification.getTime, notification.aggregationKey, notification.getContext)
end NotifyService
