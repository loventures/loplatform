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

package scaloi
package data

import scaloi.misc.TimeSource

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/** A set-like container for a person of few needs, primarily deduplication.
  */
trait Dedup[A]:

  /** Add an element to this container. */
  def +=(x: A): Unit

  /** Add a sequence of elements to this container. */
  def ++=(xs: Iterable[A]): Unit

  /** Test whether an element is present in this container. */
  def contains(a: A): Boolean
end Dedup

/** This class keeps a record of recent elements to allow for tasks such as efficient deduplication of events.
  *
  * This class is threadsafe.
  *
  * @param expiration
  *   the minimum length of time that elements should be retained
  * @param ts
  *   a source for time
  */
class BucketGenerationalDedup[A](expiration: FiniteDuration, buckets: Int, ts: TimeSource) extends Dedup[A]:
  import BucketGenerationalDedup.*

  /** The buckets. */
  private var dedups = List[TimeBucket[A]]()

  private val timeout = expiration.toMillis

  /** Add an element to this container. */
  override def +=(x: A): Unit = this ++= Seq(x)

  /** Add a sequence of elements to this container. */
  override def ++=(xs: Iterable[A]): Unit = synchronized {
    val now = ts.time
    if dedups.headOption.forall(b => b.expires <= now) then
      dedups = new TimeBucket[A](now + timeout / buckets) :: dedups.filter(b => b.expires > now - timeout)
    dedups.head.values ++= xs // no need to remove from old buckets
    ()
  }

  /** Test whether an element is present in this container. */
  override def contains(a: A): Boolean = synchronized {
    val oldest = ts.time - timeout
    dedups exists { b =>
      (b.expires >= oldest) && b.values.contains(a)
    }
  }

  /** For testing. */
  private[data] def bucketCount = dedups.size
end BucketGenerationalDedup

object BucketGenerationalDedup:

  /** Create an empty dedup.
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
    *   the new empty dedup
    */
  def empty[A](expiration: FiniteDuration, buckets: Int)(implicit ts: TimeSource): BucketGenerationalDedup[A] =
    new BucketGenerationalDedup[A](expiration, buckets, ts)

  /** A time bucket with its expiration time (when it should no longer accept elements). */
  private class TimeBucket[B](val expires: Long):
    val values = mutable.HashSet.empty[B]
end BucketGenerationalDedup
