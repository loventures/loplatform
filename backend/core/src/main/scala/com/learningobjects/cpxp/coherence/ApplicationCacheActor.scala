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
import com.learningobjects.cpxp.util.cache.Cache

import java.io.Serializable

/** This application cache actor hears distributed pub-sub notifications to remove or invalidate application cache
  * entries by key or invalidation name. These invalidations are pushed out by [Cache#remove] and [Cache#invalidate].
  */
class ApplicationCacheActor[K <: Serializable] private (cache: Cache[K, ?, ?]) extends Actor:
  import ApplicationCacheActor.*

  override def preStart(): Unit =
    logger debug s"Starting application cache coherence actor: ${cache.getName}"
    DistributedPubSub(context.system).mediator !
      Subscribe(topicName(cache), context.self)

  override val receive: Receive = {
    case SubscribeAck(_)  => logger debug "Subscribe ack"
    case Remove(key)      => onRemove(key)
    case Invalidate(name) => onInvalidate(name)
  }

  private def onRemove(key: Serializable): Unit =
    logger debug s"Received cache remove: $key"
    cache.remove(key.asInstanceOf[K], propagate = false)

  private def onInvalidate(name: String): Unit =
    logger debug s"Received cache invalidate: $name"
    cache.invalidate(name, propagate = false)
end ApplicationCacheActor

object ApplicationCacheActor:
  private final val logger = org.log4s.getLogger

  def startActor[K <: Serializable](cache: Cache[K, ?, ?])(implicit system: ActorSystem): ActorRef =
    system.actorOf(Props(new ApplicationCacheActor(cache)), cache.getName)

  def topicName(cache: Cache[?, ?, ?]): String = s"${cache.getName}Coherence"

  sealed trait Invalidation

  final case class Remove(key: Serializable) extends Invalidation

  final case class Invalidate(name: String) extends Invalidation
end ApplicationCacheActor
