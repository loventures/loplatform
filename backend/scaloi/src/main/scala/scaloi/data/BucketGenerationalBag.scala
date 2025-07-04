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

package scaloi.data

import scalaz.std.anyVal.intInstance
import scalaz.std.list.*
import scalaz.syntax.foldable.*
import scaloi.misc.TimeSource
import scaloi.syntax.any.*
import scaloi.syntax.mutableMap.*

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/** Counts approximately how many items have been added within a given time window.
  *
  * This class is not threadsafe.
  *
  * @tparam A
  *   the element type
  * @param expiration
  *   the duration for which to retain items
  * @param buckets
  *   the number of buckets
  * @param ts
  *   the time source
  */
class BucketGenerationalBag[A](expiration: FiniteDuration, buckets: Int, ts: TimeSource):
  import BucketGenerationalBag.*

  /** Bags of keys. */
  private var bags = List.empty[BagBucket[A]]

  /** Validity window for the active (head) bucket. */
  private val active = expiration / buckets

  /** Add a key to this bag. */
  def add(key: A): Unit = head.add(key)

  /** Get the approximate number of instances of a key in this bag. */
  def count(key: A): Int = bags.filter(valid(_, expiration)).foldMap(_.count(key))

  /** Get the approximate number of instances of a key added to this bag within a given time window. */
  def count(key: A, window: FiniteDuration): Int = bags.filter(valid(_, window)).foldMap(_.count(key))

  /** Get or create the active head bucket. */
  private def head: BagBucket[A] = bags.headOption.filter(valid(_, active)) getOrElse {
    new BagBucket[A](ts.time) <| { bucket =>
      bags = bucket :: bags.filter(valid(_, expiration))
    }
  }

  /** Is a bucket valid within a time window. */
  private def valid(bucket: BagBucket[A], window: FiniteDuration): Boolean =
    ts.time - bucket.created < window.toMillis

  /** For testing. */
  private[data] def bucketCount = bags.size
end BucketGenerationalBag

object BucketGenerationalBag:

  /** Create an empty bag.
    *
    * @tparam A
    *   the element type
    * @param expiration
    *   the duration for which to retain items
    * @param buckets
    *   the number of buckets
    * @param ts
    *   the time source
    * @return
    *   the nnew empty bag
    */
  def empty[A](expiration: FiniteDuration, buckets: Int)(implicit ts: TimeSource): BucketGenerationalBag[A] =
    new BucketGenerationalBag[A](expiration, buckets, ts)

  /** A bucket within the bag. */
  private class BagBucket[A](val created: Long):
    private val counts = mutable.Map.empty[A, Int].withDefaultZero

    def count(key: A): Int = counts(key)

    def add(key: A): Unit = counts.put(key, 1 + counts(key))
end BucketGenerationalBag
