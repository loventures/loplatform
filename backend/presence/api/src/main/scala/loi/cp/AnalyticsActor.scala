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

package loi.cp

import java.util.{Date, UUID}

import org.apache.pekko.actor.{Actor, ActorRef, Props}
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.event.SessionEvent
import loi.cp.presence.UserActor
import loi.cp.presence.UserActor.SessionsState
import loi.cp.util.{ActorAnalyticsService, ActorAnalyticsServiceImpl}
import scalaz.{@@, Tag}

/** An actor that listens for AnalyticsEvent and emits Caliper events
  */
class AnalyticsActor(actorAnalyticsService: ActorAnalyticsService) extends Actor:
  import AnalyticsActor.*

  override def receive: Receive = { case p: SessionsActivityEvent =>
    emitSessionEvent(p.userId, p.sessionState)
  }

  override def preStart(): Unit =
    context.system.eventStream.subscribe(self, classOf[AnalyticsEvent])

  private def emitSessionEvent(userId: Long, ss: SessionsState): Unit =
    logger.debug(s"emitting ${if ss.active then "active" else "idle"} with state $ss")

    actorAnalyticsService.emitEventWithBuilder(userId, None) { (domain, user, _, _) =>
      SessionEvent(
        id = UUID.randomUUID(),
        time = new Date(),
        source = domain.hostName,
        sessionId = None,
        actionType = if ss.active then EventActionType.BECAME_ACTIVE else EventActionType.BECAME_IDLE,
        lastActive = Some(ss.lastActive),
        becameActive = Some(ss.becameActive),
        user = user.eie,
        requestUrl = null,
        ipAddress = null,
        referrer = null,
        acceptLanguage = null,
        userAgent = null,
        authMethod = None
      )
    }
  end emitSessionEvent
end AnalyticsActor

object AnalyticsActor:

  private val logger = org.log4s.getLogger
  type Ref = ActorRef @@ AnalyticsActor
  val props: Props = Props(new AnalyticsActor(new ActorAnalyticsServiceImpl()))

  lazy val localActor: Ref = Tag.of[AnalyticsActor](CpxpActorSystem.system.actorOf(props, "analyticsActor"))

  trait AnalyticsEvent
  case class SessionsActivityEvent(userId: Long, sessionState: UserActor.SessionsState) extends AnalyticsEvent
end AnalyticsActor
