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

package com.learningobjects.cpxp.locache

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.pekko.cluster.pubsub.DistributedPubSub
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import org.hibernate.cache.spi.support.DomainDataRegionTemplate
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.persister.collection.CollectionPersister
import org.hibernate.persister.entity.EntityPersister

/** This actor listens for cache eviction messages from the cluster and applies them to the L2 caches in the local JVM
  * and the item-aware app caches.
  */
class CacheReplicationActor private (cacheName: String, cacheKey: AnyRef => AnyRef, cache: LoCache) extends Actor:
  import CacheReplicationActor.*

  override def preStart(): Unit =
    logger debug s"Starting cache replication actor: $cacheName"
    DistributedPubSub(context.system).mediator !
      Subscribe(cacheName, context.self)

  override val receive: Receive = {
    case SubscribeAck(_)  => logger debug "Subscribe ack"
    case e: EvictAll      => if !e.isLocal then onEvictAll()
    case e @ Evict(_, pk) => if !e.isLocal then onEvictEntry(pk)
  }

  private def onEvictAll(): Unit =
    logger trace s"Received evict all: $cacheName"
    cache.evictAll()

  private def onEvictEntry(pk: AnyRef): Unit =
    logger trace s"Received evict $pk: $cacheName"
    cache.evictData(cacheKey(pk))
end CacheReplicationActor

object CacheReplicationActor:
  private final val logger = org.log4s.getLogger

  def startActor(persister: EntityPersister)(implicit system: ActorSystem): ActorRef =
    system.actorOf(
      Props(
        new CacheReplicationActor(
          persister.getEntityName,
          persister.getCacheAccessStrategy.generateCacheKey(_, persister, persister.getFactory, null),
          persister.getCacheAccessStrategy.getRegion
            .asInstanceOf[DomainDataRegionTemplate]
            .getCacheStorageAccess
            .asInstanceOf[LoCache]
        )
      ),
      s"${persister.getMappedClass.getSimpleName}Replicator"
    )

  def startActor(persister: CollectionPersister)(implicit system: ActorSystem): ActorRef =
    system.actorOf(
      Props(
        new CacheReplicationActor(
          persister.getRole,
          persister.getCacheAccessStrategy.generateCacheKey(_, persister, persister.getFactory, null),
          persister.getCacheAccessStrategy.getRegion
            .asInstanceOf[DomainDataRegionTemplate]
            .getCacheStorageAccess
            .asInstanceOf[LoCache]
        )
      ),
      s"${persister.getOwnerEntityPersister.getMappedClass.getSimpleName}_${propertyName(persister.getRole)}Replicator"
    )

  private def propertyName(role: String): String = role.replaceFirst(".*\\.", "")

  /** Start the replication machinery for all caches in the Hibernate session factory. */
  def startActors(sessionFactory: SessionFactoryImpl)(implicit system: ActorSystem): Unit =
    // setup cache replication for all entities
    sessionFactory.getMetamodel.forEachEntityDescriptor { persister =>
      if persister.canReadFromCache then startActor(persister)
    }
    // setup cache replication for all collections
    sessionFactory.getMetamodel.forEachCollectionDescriptor { persister =>
      if persister.hasCache then startActor(persister)
    }
end CacheReplicationActor
