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

import org.apache.pekko.actor.{ActorContext, ActorSystem}
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator.Publish
import org.apache.pekko.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

/** Cluster-wide broadcasts.
  */
object ClusterBroadcaster:

  /** The cluster broadcast topic name. */
  final val topicName = "cluster-system"

  /** Subscribe to cluster-wide broadcasts.
    *
    * @param context
    *   subscribing actor context
    */
  def subscribe()(implicit context: ActorContext): Unit =
    DistributedPubSub(context.system).mediator !
      DistributedPubSubMediator.Subscribe(topicName, context.self)

  /** Broadcast to entire cluster.
    *
    * @param message
    *   the message
    * @param system
    *   the actor system (e.g. CpxpActorSystem.cpxpSystem)
    */
  def broadcast(message: SessionsActor.DomainMessage)(implicit system: ActorSystem): Unit =
    DistributedPubSub(system).mediator ! Publish(topicName, message)
end ClusterBroadcaster
