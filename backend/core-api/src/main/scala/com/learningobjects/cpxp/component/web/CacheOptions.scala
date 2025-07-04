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

package com.learningobjects.cpxp.component.web

import com.learningobjects.cpxp.Id

/** This object allows a request method to indicate that a response should be cached, and present information about when
  * the cache entry should be invalidated.
  */
class CacheOptions(
  /** Get the invalidation keys for this Web response.
    *
    * If an invalidation message is broadcast matching one of these keys, then the cached response will be invalidated.
    */
  val invalidationKeys: Set[String]
)

object CacheOptions:

  /** Create cache options with no invalidation information. */
  def empty: CacheOptions = new CacheOptions(Set.empty)

  /** Create cache options with a sequence of invalidation entities. */
  def apply(itemDependencies: Id*): CacheOptions =
    new CacheOptions(itemDependencies.map(_.getId.toString).toSet)

  /** Create cache options with a single invalidation entity and invalidation key. */
  def apply(itemDependency: Id, invalidationKey: String): CacheOptions =
    new CacheOptions(Set(itemDependency.getId.toString, invalidationKey))
end CacheOptions
