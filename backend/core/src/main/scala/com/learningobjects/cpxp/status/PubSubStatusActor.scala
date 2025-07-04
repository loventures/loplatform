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

package com.learningobjects.cpxp.status

import org.apache.pekko.actor.{Actor, ActorRef, Props}
import org.apache.pekko.cluster.Cluster
import org.apache.pekko.cluster.pubsub.DistributedPubSub
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import scaloi.misc.TimeSource.*

import java.util.Date
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.* // hmm

/** An actor responsible for maintaining status about the health of the pub sub system. Constantly broadcasts its
  * personal status and listens for the status of others.
  */
class PubSubStatusActor(sm: ServiceMeta) extends Actor:
  import PubSubStatusActor.*

  private val nodes = mutable.Map.empty[String, StatusUpdate]

  /** Subscribe to the topic and schedule status update.
    */
  override def preStart(): Unit =
    logger debug s"Pub sub status actor started"
    val pubSub     = DistributedPubSub(context.system).mediator
    pubSub ! Subscribe(TopicName, self)
    def announce() = pubSub ! Publish(PubSubStatusActor.TopicName, StatusUpdate(ts.date, sm.getLocalHost, leader))
    // for the first 5 minutes, announce every 5 seconds
    val initial    = context.system.scheduler.scheduleWithFixedDelay(5.seconds, 5.seconds)(() => announce())
    // then ping every 30 seconds
    context.system.scheduler.scheduleOnce(5.minutes)(initial.cancel())
    context.system.scheduler.scheduleWithFixedDelay(5.minutes, 30.seconds)(() => announce())
  end preStart

  /** Get the actor message handler.
    * @return
    *   the actor message handler
    */
  override def receive: Receive = {
    case StatusRequest(since) => onStatusRequest(since)
    case status: StatusUpdate => onStatusUpdate(status)
  }

  /** Handle a status request.
    */
  private def onStatusRequest(since: Date): Unit =
    logger info s"Status request: ${sender()} $since"
    sender() ! StatusResponse(nodes.values.filter(_.date.after(since)).toList)

  /** Handle a node reporting in.
    */
  private def onStatusUpdate(status: StatusUpdate): Unit =
    nodes.put(status.node, status)

  /** Get the leader.
    */
  private def leader: Option[String] =
    Cluster(context.system).state.leader.flatMap(_.host)
end PubSubStatusActor

/** Status actor companion.
  */
object PubSubStatusActor:

  /** The logger. */
  private final val logger = org.log4s.getLogger

  /** The cluster broadcast topic name. */
  final val TopicName = "cluster-status"

  /** The actor name. */
  final val ActorName = "pubSubStatus"

  private var localActorRef: ActorRef = scala.compiletime.uninitialized

  /** The local actor reference. */
  def localActor: Option[ActorRef] = Option(localActorRef)

  /** Initialize this actor. */
  def initialize(sm: ServiceMeta): Unit =
    localActorRef = CpxpActorSystem.system.actorOf(Props(new PubSubStatusActor(sm)), ActorName)

  /** A status update message.
    */
  case class StatusUpdate(date: Date, node: String, leader: Option[String])

  /** A status request message. The response is a {StatusResponse}.
    */
  case class StatusRequest(since: Date)

  /** A status check response.
    * @param nodes
    *   the nodes that have identified themselves on the topic
    */
  case class StatusResponse(nodes: List[StatusUpdate])
end PubSubStatusActor
