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

package loi.cp.status

import com.learningobjects.cpxp.util.cache.{BucketGenerationalCache, Entry}

import java.util.Date
import scala.collection.mutable
import scala.concurrent.duration.*

/** A replicated cache that accumulates the invalidations that it hears about. */
class ClusterStatusCache
    extends BucketGenerationalCache[String, ClusterStatusEntry, Entry[String, ClusterStatusEntry]](
      itemAware = false,
      replicated = true,
      timeout = 0.seconds
    ):
  private val invalidations = mutable.Map.empty[String, Date]

  override def invalidate(key: String, propagate: Boolean): Unit =
    invalidations.put(key, new Date)
    super.invalidate(key, propagate)

  def hosts(since: Date): List[String] = invalidations.toList.filter(_._2.after(since)).map(_._1)
end ClusterStatusCache

class ClusterStatusEntry
