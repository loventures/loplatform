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

import java.util as ju

import scala.jdk.CollectionConverters.*

abstract class Entry[K, V](
  val key: K,
  val value: V,
  val invalidationKeys: Set[String] = Set.empty
):
  protected def this(key: K, value: V, invalidationKeys: ju.Collection[String]) =
    this(key, value, invalidationKeys.asScala.toSet[String])

  def isStale = false

  def ref(): this.type   = this
  def deref(): this.type = this
end Entry
