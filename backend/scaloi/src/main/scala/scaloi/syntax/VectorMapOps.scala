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

package scaloi.syntax

import scala.collection.immutable.VectorMap

final class VectorMapOps[K, V](private val self: VectorMap[K, V]) extends AnyVal:

  /** If k is not in the map then add entry (k, v), otherwise do nothing
    */
  def meeklyUpdated(k: K, v: V): VectorMap[K, V] = self.updatedWith(k) {
    case prior: Some[V] => prior
    case None           => Some(v)
  }

object VectorMapOps extends ToVectorMapOps

trait ToVectorMapOps:
  import language.implicitConversions

  @inline
  final implicit def ToVectorMapOps[K, V](self: VectorMap[K, V]): VectorMapOps[K, V] = new VectorMapOps[K, V](self)
