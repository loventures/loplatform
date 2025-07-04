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

import java.util.Date
import org.apache.pekko.actor.{Actor, ActorRef, Cancellable, Props, Terminated}
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.scala.actor.{ClusterActors, CpxpActorSystem}
import com.learningobjects.cpxp.util.Box
import io.prometheus.client.Gauge
import loi.apm.Apm
import loi.cp.presence.UserActor.LastActive
import scaloi.misc.TimeSource
import scaloi.syntax.date.*
import scaloi.syntax.mutableMap.*

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scalaz.syntax.tag.*
import scalaz.{@@, Tag, Tags}

/** The users actor. This is a singleton actor responsible for the management of all user actors in the system.
  *
  * @param now
  *   a source for the current time
  */
class UsersActor(implicit now: TimeSource) extends Actor:
  import UsersActor.*

  /** A map of user ids to the associated user actors.
    */
  val users: mutable.Map[Long, UserActor.Ref] = mutable.Map.empty

  /** A map of user actor refs to the last time they were known to be active (whether visibly or not).
    */
  val accessTimes: mutable.Map[ActorRef, Date @@ Tags.MaxVal] = mutable.Map.empty

  /** A scheduled log event. */
  private val logStuff = Box.empty[Cancellable]

  /** Schedule that we get a `LogStatistics` ping every once in a while.
    */
  override def preStart(): Unit =
    logStuff.value =
      context.system.scheduler.scheduleWithFixedDelay(LogStatisticsInterval, LogStatisticsInterval, self, LogStatistics)

  override def postStop(): Unit =
    logStuff foreach { _.cancel() }

  /** The actor message handler.
    */
  override val receive: Receive = {
    case GetUser(id)                            => onGetUser(id)
    case FollowUsers(ids*)                      => onFollowUsers(ids)
    case UnfollowUsers(ids*)                    => onUnfollowUsers(ids)
    case DeliverMessage(messageType, body, ids) => onDeliverMessage(messageType, body, ids)
    case LogStatistics                          => onLogStatistics()
    case LastActive(Some(lastAccess))           => onLastActive(lastAccess)
    case Terminated(actor)                      => onTerminated(actor)
  }

  /** Get a user actor.
    *
    * @param id
    *   the user id
    */
  private def onGetUser(id: Long): Unit =
    sender() ! UserRef(id, getOrCreateUser(id))

  /** Follow a set of users.
    *
    * @param ids
    *   the user ids
    */
  private def onFollowUsers(ids: Seq[Long]): Unit =
    ids foreach { id =>
      getOrCreateUser(id) `forward` UserActor.Follow
    }

  /** Unfollow a set of users.
    *
    * @param ids
    *   the user ids
    */
  private def onUnfollowUsers(ids: Seq[Long]): Unit =
    ids foreach { id =>
      users.get(id) foreach { user =>
        user `forward` UserActor.Unfollow
      }
    }

  /** Deliver a message to a set of users.
    *
    * @param eventType
    *   the event type
    * @param message
    *   the message
    * @param ids
    *   the user ids
    */
  private def onDeliverMessage(eventType: String, message: Any, ids: Array[Long]): Unit =
    ids foreach { id =>
      users.get(id) foreach { user =>
        logger.debug(s"UsersActor $self deliver $eventType / $message to user $id / $user")
        user `forward` PresenceActor.DeliverMessage(eventType, message)
      }
    }

  /** Unmap a user actor upon its death.
    *
    * @param actor
    *   the user actor
    */
  private def onTerminated(actor: ActorRef): Unit =
    users.filterInPlace { case (_, user) => user.unwrap != actor }
    accessTimes.remove(actor)

  /** Get or create a user actor.
    *
    * @param id
    *   the user id
    * @return
    *   the user actor
    */
  private def getOrCreateUser(id: Long): UserActor.Ref =
    users.getOrElseUpdate(id, context `watch` UserActor.create(id))

  /** Modify the last access time for a user actor.
    */
  private def onLastActive(date: Date): Unit =
    accessTimes.append(sender(), Tag.of[Tags.MaxVal](date))

  /** Handle a `LogStatistics` command by calculating various statistics and sending them to APM.
    */
  private def onLogStatistics(): Unit =
    def within(dur: Duration)(when: Date @@ Tags.MaxVal): Boolean =
      (now.date - when.unwrap) < dur

    val total  = accessTimes.size
    val active = accessTimes.values.count(within(ActiveDuration))
    val recent = accessTimes.values.count(within(RecentDuration))

    logger info s"User log: $total total, $active active, $recent recent"
    Apm.recordMetric("Presence/totalUsers", total.toFloat)
    Apm.recordMetric("Presence/activeUsers", active.toFloat)
    Apm.recordMetric("Presence/recentUsers", recent.toFloat)

    presenceTotalUsers.set(total)
    presenceActiveUsers.set(active)
    presenceRecentUsers.set(recent)
  end onLogStatistics
end UsersActor

/** Users actor companion.
  */
object UsersActor:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** A users actor reference. */
  type Ref = ActorRef @@ UsersActor

  /** Properties to create a new users actor. */
  val props: Props = Props(new UsersActor)

  // TODO: should this shard by context... should there by a per-domain contexts actor.. singletons bad..

  /** The singleton users actor. */
  lazy val clusterActor: Ref =
    Tag.of[UsersActor](ClusterActors.singleton(props, "users")(using CpxpActorSystem.system))

  /** Request to get a reference to a user actor. The actor will be created if it does not exist. No validation of the
    * user id is done.
    *
    * @param id
    *   the user id
    */
  case class GetUser(id: Long)

  /** Response with a user reference.
    *
    * @param id
    *   the user id
    * @param actor
    *   the user actor
    */
  case class UserRef(id: Long, actor: UserActor.Ref)

  /** Request to follow a set of users. The caller will be subscribed to receive presence updates from all the users.
    *
    * @param ids
    *   the user ids
    */
  case class FollowUsers(ids: Long*)

  /** Request to unfollow a set of users. The caller will be unsubscribed from presence updates for all the users.
    *
    * @param ids
    *   the user ids
    */
  case class UnfollowUsers(ids: Long*)

  /** Deliver a message on to users if they are active.
    *
    * @param eventType
    *   the event type
    * @param message
    *   the message
    * @param userIds
    *   the user ids
    */
  case class DeliverMessage(eventType: String, message: Any, userIds: Array[Long])

  /** Calculate and log statistics about recently active users.
    *
    * Sent on a delay by Pekko; not to be sent manually.
    */
  private case object LogStatistics

  /** Length of time before we consider a user inactive. */
  private val ActiveDuration = 5.minutes

  /** Length of time before we consider a user not recent. */
  private val RecentDuration = 30.minutes

  /** Interval at which to log presence statistics. */
  private val LogStatisticsInterval = 1.minute

  private val presenceTotalUsers: Gauge  =
    Gauge.build.name("cp_presence_users").help("the number of presence users").register
  private val presenceActiveUsers: Gauge =
    Gauge.build.name("cp_presence_active_users").help("the number of active presence users").register
  private val presenceRecentUsers: Gauge =
    Gauge.build.name("cp_presence_recent_users").help("the number of presence recent users").register
end UsersActor
