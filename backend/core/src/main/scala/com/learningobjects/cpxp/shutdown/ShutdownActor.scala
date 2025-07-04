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

package com.learningobjects.cpxp.shutdown

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator.Publish
import org.apache.pekko.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import scaloi.syntax.AnyOps.*

import scala.concurrent.duration.FiniteDuration

/** An actor responsible for shutting the system down upon request.
  */
class ShutdownActor(doShutdown: () => Unit) extends Actor:
  import ShutdownActor.*

  /** Subscribe to the topic.
    */
  override def preStart(): Unit =
    logger debug s"Shutdown actor started"
    DistributedPubSub(context.system).mediator !
      DistributedPubSubMediator.Subscribe(TopicName, self)

  /** Get the actor message handler.
    * @return
    *   the actor message handler
    */
  override def receive: Receive = {
    case ShutdownRequest(delay) => onShutdownRequest(delay)
    case ShutdownNow            => onShutdownNow()
  }

  /** Handle a delayed shutdown request.
    */
  private def onShutdownRequest(delay: FiniteDuration): Unit =
    logger info s"Shutdown request: $delay"
    context.system.scheduler.scheduleOnce(delay, self, ShutdownNow)(using context.dispatcher, self)

  /** Handle an immediate shutdown request.
    */
  private def onShutdownNow(): Unit =
    logger info s"Shutdown now"
    // I want:
    //     new Thread(doShutdown.apply _, "ShutterDowner").start()
    // but SAM won't let me!
    new Thread(() => doShutdown(), "ShutterDowner").start()
end ShutdownActor

/** Shutdown actor companion.
  */
object ShutdownActor:

  /** The logger. */
  private final val logger = org.log4s.getLogger

  /** The cluster broadcast topic name. */
  final val TopicName = "cluster-shutdown"

  /** The actor name. */
  final val ActorName = "shutdown"

  /** The shutdown attribute. */
  final val ShutdownAttribute = "shutdown"

  private var localActorRef: ActorRef = scala.compiletime.uninitialized

  /** The local actor reference. */
  def localActor: Option[ActorRef] = Option(localActorRef)

  /** Initialize this actor. */
  def initialize(shutterdowner: () => Unit): ActorRef =
    CpxpActorSystem.system.actorOf(Props(new ShutdownActor(shutterdowner)), ActorName) <| { localActorRef = _ }

  /** Broadcast a shutdown request. */
  def broadcastShutdownRequest(delay: FiniteDuration)(implicit actorSystem: ActorSystem): Unit =
    DistributedPubSub(actorSystem).mediator ! Publish(TopicName, ShutdownRequest(delay))

  /** A delayed shutdown request message.
    */
  case class ShutdownRequest(delay: FiniteDuration)

  /** An immediate shutdown request message.
    */
  case object ShutdownNow
end ShutdownActor
