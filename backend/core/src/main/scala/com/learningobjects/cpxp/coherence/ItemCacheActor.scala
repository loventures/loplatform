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

package com.learningobjects.cpxp.coherence

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.pekko.cluster.pubsub.DistributedPubSub
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import com.learningobjects.cpxp.locache.AppCacheSupport

import java.lang as jl

/** This item cache actor hears distributed pub-sub notifications to invalidate application cache entries dependent on a
  * particular item PK. These invalidations are pushed out by [QueryService#evict], [Facade#invalidate] and
  * [Facade#invalidateParent].
  *
  * Note that these requests are parallel to the ehcache-based item-cache invalidations. They allow us to achieve the
  * same effect without having to make persistence changes.
  */
class ItemCacheActor private extends Actor:
  import ItemCacheActor.*

  override def preStart(): Unit =
    logger debug "Starting item cache coherence actor"
    DistributedPubSub(context.system).mediator !
      Subscribe(TopicName, context.self)

  override val receive: Receive = {
    case SubscribeAck(_) => logger debug "Subscribe ack"
    case pk: jl.Long     => onInvalidate(pk)
  }

  private def onInvalidate(pk: jl.Long): Unit =
    logger debug s"Received item invalidation: $pk"
    AppCacheSupport.removeItem(pk)
end ItemCacheActor

object ItemCacheActor:
  private final val logger = org.log4s.getLogger

  def startActor()(implicit system: ActorSystem): ActorRef =
    system.actorOf(Props(new ItemCacheActor), "ItemCache")

  final val TopicName = "ItemCacheCoherence"
