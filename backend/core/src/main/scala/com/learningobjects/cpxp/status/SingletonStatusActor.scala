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
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.scala.actor.{ClusterActors, CpxpActorSystem}
import scaloi.syntax.AnyOps.*

/** An actor responsible for responding to status requests. This is used to provide checking of actor system pub sub
  * function within the cluster.
  */
class SingletonStatusActor(sm: ServiceMeta) extends Actor:
  import SingletonStatusActor.*

  /** Log startup information.
    */
  override def preStart(): Unit =
    logger debug s"Singleton status actor started"

  /** Get the actor message handler.
    * @return
    *   the actor message handler
    */
  override def receive: Receive = { case StatusRequest =>
    onStatusRequest()
  }

  /** Handle a status request.
    */
  private def onStatusRequest(): Unit =
    logger info s"Status request: ${sender()}"
    sender() ! StatusResponse(sm.getLocalHost)
end SingletonStatusActor

/** Status actor companion.
  */
object SingletonStatusActor:

  /** The logger. */
  private final val logger = org.log4s.getLogger

  private var _clusterActor: ActorRef = scala.compiletime.uninitialized

  /** The cluster actor reference. */
  def clusterActor: Option[ActorRef]        = Option(_clusterActor)
  def initialize(sm: ServiceMeta): ActorRef =
    ClusterActors.singleton(Props(new SingletonStatusActor(sm)), "singletonStatus")(using
      CpxpActorSystem.system
    ) <| (actor => _clusterActor = actor)

  /** A status request message. The response is a {StatusResponse}.
    */
  case object StatusRequest

  /** A status check response.
    * @param node
    *   the node name
    */
  case class StatusResponse(node: String)
end SingletonStatusActor
