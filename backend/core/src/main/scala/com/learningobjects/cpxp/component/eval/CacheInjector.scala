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

package com.learningobjects.cpxp.component.eval

import argonaut.*
import argonaut.Argonaut.*
import com.learningobjects.cpxp.coherence.ApplicationCacheActor
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import com.learningobjects.cpxp.util.ManagedUtils
import com.learningobjects.cpxp.util.cache.Cache
import de.tomcat.juli.LogMeta
import scaloi.syntax.AnyOps.*

import scala.collection.mutable

/** Support for injecting caches. When a cache is created, any associated replication machinery is also started.
  */
object CacheInjector:
  private final val logger = org.log4s.getLogger

  type AnyCache = Cache[?, ?, ?]

  private final val cacheMap = mutable.Map.empty[Class[?], AnyCache]

  def caches: Seq[AnyCache] = synchronized {
    cacheMap.values.toSeq
  }

  def getCache[T <: AnyCache](tpe: Class[T]): T = synchronized {
    tpe.cast(cacheMap.getOrElseUpdate(tpe, newCache(tpe)))
  }

  private def newCache[T <: AnyCache](tpe: Class[T]): T =
    ManagedUtils.newInstance(tpe) <| { cache =>
      // In the current startup we start pekko simultaneously with
      // DIing all of our legacy services. That means that pekko isn't DIable.
      if cache.replicated then // the horrible cast is to avoid a compiler error
        CpxpActorSystem.onActorSystem(as =>
          ApplicationCacheActor.startActor(cache.asInstanceOf[Cache[Serializable, ?, ?]])(using as)
        )
    }

  def logStuff(): Unit =
    def cacheInfo(cache: AnyCache): Json = Json(
      "elements"     := cache.getSize,
      "invalidation" := cache.getInvalidationSize.toString,
      "hits"         := cache.getHitCount,
      "misses"       := cache.getMissCount
    )
    LogMeta.let(caches.map(cache => cache.getName -> cacheInfo(cache))*)(logger.info("Cache log"))
    caches.foreach(_.resetStatistics())
end CacheInjector
