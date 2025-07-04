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

package loi.cp.presence

import org.apache.pekko.actor.{Actor, ActorRef, ActorRefFactory, Props}
import loi.cp.analytics.entity.ExternallyIdentifiableEntity
import loi.cp.analytics.event.TimeSpentEvent2
import loi.cp.presence.PresenceActor.Heartbeat
import loi.cp.presence.SceneActor.{InContextWithEdgePath, SceneId}
import loi.cp.util.{ActorAnalyticsService, ActorAnalyticsServiceImpl}
import scalaz.{@@, Tag}
import scaloi.syntax.DateOps.*

import java.util.{Date, UUID}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

/** Actor that keeps track of entered and idling scenes for a single user in a single session. As scenes are viewed the
  * last seen active time is updated for each scene. Scenes that go are no longer current get removed and trigger a
  * TimeSpent analytic event based on the first time we saw the scene.
  */
class TimeOnTaskActor(
  actorAnalyticsService: ActorAnalyticsService,
  userId: Long,
  sessionPk: Long
) extends Actor:
  import TimeOnTaskActor.*
  implicit val ec: ExecutionContext = ExecutionContext.global

  private var state: Option[SceneTime] = None

  override def receive: Receive = {
    case SceneActivity(sceneIds, heartbeat) => onSceneActivity(sceneIds, heartbeat)
    case IdleCheck                          => onIdleCheck()
  }

  // TODO: I drop state if the session is closed before my idle timer kicks in

  /** Records activity that should be coming consistently from the front-end. */
  private def onSceneActivity(ids: Set[SceneId], heartbeat: Heartbeat): Unit =
    state = state.map(s => s.copy(lastSeen = heartbeat.time, activeMillis = s.activeMillis + heartbeat.activeMillis))
    // Assumes you are only ever in one piece of content.
    val contextWithEdgePath = ids collectFirst { case cwep: InContextWithEdgePath => cwep }
    if state.map(_.scene) != contextWithEdgePath then // moved to a new scene
      state.foreach(sendAnalytic)
      state = contextWithEdgePath map { scene => SceneTime(scene, heartbeat.time, 0) }
    scheduleIdleCheck()

  /** Checks for idle activity on scenes and schedules analytics or new idle timeouts */
  private def onIdleCheck(): Unit =
    state.filter(new Date() - _.lastSeen >= IdleTimeout) foreach { time =>
      sendAnalytic(time)
      state = None
    }
    scheduleIdleCheck()

  private def sendAnalytic(time: SceneTime): Unit =
    // this is for when a page is opened in a background tab
    // and never brought to foreground. We would emit 0 durationSpent
    // evemts in this case. Instead, I just don't.
    if time.activeMillis > 0 then
      actorAnalyticsService.emitEventWithBuilder(userId, Some(time.scene.context)) { (domain, user, course, commit) =>
        TimeSpentEvent2(
          id = UUID.randomUUID(),
          time = time.lastSeen,
          source = domain.hostName,
          sessionId = Some(sessionPk),
          user = user,
          context = course.getOrElse(ExternallyIdentifiableEntity(time.scene.context, None)),
          commitId = commit,
          edgePath = Some(time.scene.edgePath.toString),
          assetId = Some(time.scene.assetId),
          durationSpent = time.activeMillis,
          maintenance = None,
          originSectionId = None,
        )
      }

  private def scheduleIdleCheck(): Unit =
    if state.nonEmpty then context.system.scheduler.scheduleOnce(IdleTimeout, self, IdleCheck)
end TimeOnTaskActor

object TimeOnTaskActor:

  /** Reference to the time on task actor */
  type Ref = ActorRef @@ TimeOnTaskActor

  final case class SceneActivity(sceneIds: Set[SceneId], heartbeat: Heartbeat)

  private case object IdleCheck

  private val IdleTimeout = 3.minutes

  private final case class SceneTime(scene: InContextWithEdgePath, lastSeen: Date, activeMillis: Long)

  /** Create a new TimeOnTask Actor
    *
    * @param user
    *   pk of the user associated with the session
    * @param sessionPk
    *   pk of the users's session
    * @return
    *   the new time on task actor
    */
  def create(
    user: Long,
    sessionPk: Long,
  )(implicit factory: ActorRefFactory): Ref =
    Tag.of[TimeOnTaskActor] {
      factory.actorOf(
        Props(new TimeOnTaskActor(new ActorAnalyticsServiceImpl(), user, sessionPk))
      )
    }
end TimeOnTaskActor
