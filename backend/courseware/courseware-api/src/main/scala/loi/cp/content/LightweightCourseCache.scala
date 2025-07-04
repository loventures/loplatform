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

package loi.cp.content

import com.learningobjects.cpxp.util.cache.{BucketGenerationalCache, Entry as CacheEntry}
import loi.cp.content.LightweightCourseCache.*
import loi.cp.course.lightweight.Lwc
import scaloi.syntax.any.*

import scala.concurrent.duration.*

abstract class LightweightCourseCache[V >: Null]
    extends BucketGenerationalCache[Key, (Long, V), Entry[(Long, V)]](
      itemAware = false,
      replicated = false,
      timeout = 5.minutes,
    ):

  def getOrComputeIfStaleGeneration(lwc: Lwc)(compute: () => V): V =
    lwc.generation.fold(compute()) { generation =>
      def computeAndStore(): V = compute().tap(v => put(new Entry(Key(lwc.id), (generation, v))))
      get(Key(lwc.id)).fold(computeAndStore()) {
        case (gen, v) if gen == generation => v
        case _                             => computeAndStore()
      }
    }
end LightweightCourseCache

object LightweightCourseCache:
  case class Key(courseId: Long)                   extends Serializable
  final class Entry[V >: Null](key: Key, value: V) extends CacheEntry[Key, V](key, value)
  object Entry:
    def apply[V >: Null](key: Key, value: V): Entry[V] = new Entry(key, value)
