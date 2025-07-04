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

package loi.cp.throttle

import org.apache.pekko.actor.{Actor, ActorRef, Props}
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator.Publish
import org.apache.pekko.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import scalaz.{@@, Tag}
import scaloi.data.BucketGenerationalBag

import scala.concurrent.duration.*

/** An actor responsible for throttling access to some vulnerable services. Counts the number of accesses to a resource
  * within the last hour to a granularity of about 5 minutes. Cluster broadcast is used to maintain some degree of
  * cluster-wide consistency.
  */
class ThrottleActor extends Actor:
  import ThrottleActor.*

  /** Current counts. */
  private val counts = BucketGenerationalBag.empty[String](60.minutes, 12)

  /** Log startup information.
    */
  override def preStart(): Unit =
    logger info s"Throttle singleton started"
    DistributedPubSub(context.system).mediator !
      DistributedPubSubMediator.Subscribe(topicName, context.self)

  /** Get the actor message handler.
    * @return
    *   the actor message handler
    */
  override def receive: Receive = {
    case ThrottleRequest(resource, minutes)        => onThrottleRequest(resource, minutes)
    case DistributedPubSubMediator.SubscribeAck(_) =>
    case resource: String                          => onResource(resource)
  }

  /** Handle a throttle request.
    *
    * @param resource
    *   the resource
    * @param window
    *   the duration
    */
  private def onThrottleRequest(resource: String, window: Long): Unit =
    counts.add(resource)
    val count = counts.count(resource, window.minutes)
    logger debug s"Throttle request: $resource ($window) -> $count"
    sender() ! ThrottleResponse(count)

    DistributedPubSub(context.system).mediator ! Publish(topicName, resource)

  /** Handle a throttle notification.
    *
    * @param resource
    *   the resource that was accessed
    */
  private def onResource(resource: String): Unit =
    logger debug s"Throttle notification: $resource"
    counts.add(resource)
end ThrottleActor

/** Throttle task actor companion.
  */
object ThrottleActor:

  /** The logger. */
  private final val logger = org.log4s.getLogger

  /** A throttle actor reference. */
  type Ref = ActorRef @@ ThrottleActor

  /** The throttle topic name. */
  final val topicName = "throttle"

  /** The local actor reference. */
  lazy val localActor: Ref =
    Tag.of[ThrottleActor](CpxpActorSystem.system.actorOf(Props(new ThrottleActor), "throttle"))

  /** Record an access to a throttled resource. Returns a count of the number of accesses within a chosen time window.
    *
    * @param resource
    *   the resource being accessed
    * @param window
    *   the window, in minutes, within which to count accesses
    */
  case class ThrottleRequest(resource: String, window: Long)

  /** A throttle response. Indicates the number of recent accesses.
    */
  case class ThrottleResponse(count: Int)
end ThrottleActor
