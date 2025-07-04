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

import org.apache.pekko.cluster.pubsub.DistributedPubSub
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator.Publish
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import com.learningobjects.cpxp.service.item.Item
import de.mon.{DeMonitor, StatisticType}
import org.hibernate.cache.spi.access.SoftLock
import org.hibernate.cache.spi.support.DomainDataStorageAccess
import org.hibernate.engine.spi.SharedSessionContractImplementor

import java.util.concurrent.ConcurrentHashMap
import java.util.function as juf
import scala.concurrent.duration.*

/** A crude hibernate cache implementation that knows when to tell Campus Pack about interesting things that it may act
  * upon said information and perhaps broadcast it to the world.
  */
class LoCache(cacheName: String) extends DomainDataStorageAccess:
  import LoCache.*

  private val cache       = new ConcurrentHashMap[AnyRef, LoEntry]()
  private val tableName   = cacheName.replaceFirst(".*\\.", "")
  private val isItemCache = cacheName == classOf[Item].getName
  private var purged      = System.currentTimeMillis

  override def getFromCache(key: AnyRef, session: SharedSessionContractImplementor): AnyRef =
    val entry = cache.get(key)
    if (entry ne null) && (entry.expires > System.currentTimeMillis) then
      DeMonitor.recordGlobalStatistic(StatisticType.LoCacheHit, cacheName, 0L)
      entry.value
    else null

  override def putFromLoad(key: AnyRef, value: AnyRef, session: SharedSessionContractImplementor): Unit =
    cache.put(key, LoEntry(value, System.currentTimeMillis + MaxAge))

  override def putIntoCache(key: AnyRef, value: AnyRef, session: SharedSessionContractImplementor): Unit =
    val now      = System.currentTimeMillis
    val existing = cache.put(key, LoEntry(value, now + MaxAge))
    if (existing ne null) && existing.value.isInstanceOf[SoftLock]
    then // Hibernate puts in a soft lock during the locked operation then the new value
      val id = getEntityId(key)
      logger trace s"Cache invalidation: $cacheName#$id"
      if isItemCache then AppCacheSupport.removeItem(id.asInstanceOf[java.lang.Long])
      DeMonitor.recordGlobalStatistic(StatisticType.LoCacheInvalidate, cacheName, 0L)
      // We only replicate an eviction if Hibernate locked an entity for update and has now cached the final data
      DistributedPubSub(CpxpActorSystem.system).mediator ! Publish(cacheName, Evict(id))
    if now - purged > MaxAge then
      cache.values.removeIf(entry => entry.expires <= now)
      purged = now
  end putIntoCache

  override def contains(key: AnyRef): Boolean =
    val entry = cache.get(key)
    (entry ne null) && (entry.expires > System.currentTimeMillis)

  override def evictData(): Unit =
    evictAll()
    // Hibernate calls `evictData`, and when it does we replicate that eviction
    DistributedPubSub(CpxpActorSystem.system).mediator ! Publish(cacheName, EvictAll.instance)

  /** Local eviction without replication notification. */
  def evictAll(): Unit =
    logger trace s"Evict data: $cacheName"
    cache.clear()
    DeMonitor.tableEvicted(tableName)
    if isItemCache then AppCacheSupport.removeAll()

  override def evictData(key: AnyRef): Unit =
    logger trace s"Evict data: $cacheName#$key"
    cache.remove(key)
    if isItemCache then AppCacheSupport.removeItem(getEntityId(key).asInstanceOf[java.lang.Long])

  override def release(): Unit = cache.clear()
end LoCache

final case class LoEntry(value: AnyRef, expires: Long)

object LoCache:
  final val logger = org.log4s.getLogger
  final val MaxAge = 10.minutes.toMillis

  final val caches = new ConcurrentHashMap[String, LoCache]()

  def getOrCreate(cacheName: String): LoCache = caches.computeIfAbsent(cacheName, new LoCache(_))

  def forEach(f: juf.Consumer[LoCache]): Unit = caches.values.forEach(f)

  def shutdown(): Unit = caches.clear()
end LoCache

/** For tests. It is a cache, but simple. */
class SimpleCache extends DomainDataStorageAccess:
  private val cache = new ConcurrentHashMap[AnyRef, AnyRef]

  override def getFromCache(key: AnyRef, session: SharedSessionContractImplementor): AnyRef = cache.get(key)

  override def putIntoCache(key: AnyRef, value: AnyRef, session: SharedSessionContractImplementor): Unit =
    cache.put(key, value)

  override def contains(key: AnyRef): Boolean = cache.containsKey(key)

  override def evictData(): Unit = cache.clear()

  override def evictData(key: AnyRef): Unit = cache.remove(key)

  override def release(): Unit = cache.clear()

  def size(): Int = cache.size()
end SimpleCache
