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

import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*
import scaloi.syntax.collection.*

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/** A simple expiring map-like structure that efficiently drops all expired elements upon insert. Expiration time is
  * approximate.
  */
class BucketGenerationalMap[A, B](expiration: FiniteDuration, bucketCount: Int, ts: TimeSource):
  import BucketGenerationalMap.*

  private val buckets = mutable.ArrayBuffer.empty[TimeBucket[A, B]]

  private val timeout = expiration.toMillis

  def clear(): Unit = synchronized {
    buckets.clear()
  }

  def remove(key: A): Option[B] = synchronized {
    buckets.findMap(_.values.remove(key))
  }

  def get(key: A): Option[B] = synchronized {
    val oldest = ts.time - timeout
    // find in any non-expired bucket
    buckets.findMap(b => (b.expires >= oldest).flatOption(b.values.get(key)))
  }

  def put(key: A, value: B): Option[B] = synchronized {
    val now = ts.time
    // ensure the head bucket is valid and drop any expired buckets off the tail
    if buckets.headOption.forall(_.expires <= now) then
      val index = buckets.indexWhere(_.expires > now - timeout)
      if index >= 0 then buckets.takeInPlace(index)
      buckets.prepend(new TimeBucket[A, B](now + timeout / bucketCount))
    // put it in the head bucket and if that did not contain a mapping, remove and return any mapping from older buckets
    buckets.head.values.put(key, value).orElse(buckets.tail.findMap(_.values.remove(key)))
  }
end BucketGenerationalMap

object BucketGenerationalMap:

  /** Create an empty map.
    *
    * @tparam A
    *   the key type
    * @tparam B
    *   the value type
    * @param expiration
    *   the duration for which to retain items
    * @param buckets
    *   the number of buckets
    * @param ts
    *   the time source
    * @return
    *   the new empty map
    */
  def empty[A, B](expiration: FiniteDuration, buckets: Int)(implicit ts: TimeSource): BucketGenerationalMap[A, B] =
    new BucketGenerationalMap[A, B](expiration, buckets, ts)

  /** A time bucket with its expiration time (when it should no longer accept elements). */
  private class TimeBucket[A, B](val expires: Long):
    final val values = mutable.HashMap.empty[A, B]
end BucketGenerationalMap
