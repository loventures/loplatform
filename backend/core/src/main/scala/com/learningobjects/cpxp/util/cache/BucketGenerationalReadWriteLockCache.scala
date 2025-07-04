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

package com.learningobjects.cpxp.util.cache

import com.learningobjects.cpxp.coherence.DeferredCoherence
import scalaz.std.anyVal.*
import scalaz.std.list.*
import scalaz.std.tuple.*
import scalaz.syntax.foldable.*
import scaloi.misc.TimeSource
import scaloi.syntax.any.*
import scaloi.syntax.foldable.*
import scaloi.syntax.readWriteLock.*

import java.io.Serializable
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}
import scala.concurrent.duration.FiniteDuration

/** @param timeout
  *   the minimum length of time (in milliseconds) that elements should be retained
  * @param ts
  *   a source for time
  */
class BucketGenerationalReadWriteLockCache[K <: Serializable, V >: Null, E <: Entry[K, V]](
  val itemAware: Boolean,
  val replicated: Boolean,
  timeout: FiniteDuration
)(implicit ts: TimeSource)
    extends Cache[K, V, E]:
  import BucketGenerationalReadWriteLockCache.*

  private val lock: ReadWriteLock = new ReentrantReadWriteLock()

  /** The buckets. */
  private var buckets = List[CacheBucket[K, V]]()

  private var hitCount  = 0
  private var missCount = 0

  override val getMaxAge: Long = timeout.toMillis

  override def getName: String = getClass.getSimpleName

  override def getHitCount: Int = hitCount

  override def getMissCount: Int = missCount

  override def resetStatistics(): Unit =
    lock writing {
      hitCount = 0
      missCount = 0
    }

  override def getSize: Int =
    lock reading {
      buckets.map(_.size).sum
    }

  override def getInvalidationSize: (Int, Int) =
    lock reading {
      buckets.map(_.invalidationSize).suml
    }

  override def put(entry: E): Unit =
    lock writing {
      if head.add(entry.ref()).isEmpty then // if not already in head, may be in tail so remove from tail
        buckets.tail foreach { bucket =>
          bucket.remove(entry.key).map(_.deref())
        }
    }

  private def head: CacheBucket[K, V] =
    val now = ts.time
    buckets.headOption.filter(_.stillValid(now)) getOrElse {
      new CacheBucket[K, V](now + getMaxAge / Buckets) <| { bucket =>
        buckets = bucket :: buckets.filter(b => b.stillValid(now - getMaxAge))
      }
    }

  override def invalidate(inv: String, propagate: Boolean): Unit =
    lock writing {
      buckets foreach { _.invalidate(inv) }
      if propagate && replicated then DeferredCoherence.invalidateEntry(this, inv)
    }

  /** Return just the buckets that could potentially contain non-expired entries. */
  private def validBuckets: List[CacheBucket[K, V]] =
    val oldest = ts.time - getMaxAge
    buckets.takeWhile(_.stillValid(oldest))

  override def test(key: K): Boolean =
    lock reading {
      validBuckets exists { b =>
        b.contains(key)
      }
    }

  override def getKeys: List[K] =
    lock reading {
      validBuckets.flatMap(_.keys)
    }

  override def get(key: K): Option[V] =
    lock reading {
      validBuckets.findMap(_.get(key)).map(_.ref())
    } match
      case None =>
        logger.debug(s"Cache entry miss: $key")
        missCount += 1
        None

      case Some(entry) if entry.isStale => // assumes the isStale call is expensive and not to be synchronized
        logger.debug(s"Cache entry stale: $key")
        missCount += 1
        remove(key)
        entry.deref()
        None

      case Some(entry) =>
        logger.debug(s"Cache entry hit: $key")
        hitCount += 1
        Some(entry.value)

  override def remove(key: K, propagate: Boolean): Unit =
    lock writing {
      buckets
        .findMap(_.remove(key))
        .foreach(_.deref())
      if propagate && replicated then DeferredCoherence.removeEntry(this, key)
    }

  override def clear(): Unit =
    lock writing {
      buckets.flatMap(_.values).map(_.deref())
      buckets = List.empty
    }
end BucketGenerationalReadWriteLockCache

object BucketGenerationalReadWriteLockCache:
  private final val logger = org.log4s.getLogger

  /** The number of buckets. */
  final val Buckets = 3
