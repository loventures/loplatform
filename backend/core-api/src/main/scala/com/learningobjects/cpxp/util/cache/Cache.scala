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

import scaloi.syntax.AnyOps.*

trait Cache[K, V, E <: Entry[K, V]]:
  val replicated: Boolean

  val itemAware: Boolean

  def getName: String

  def getMaxAge: Long

  def getHitCount: Int

  def getMissCount: Int

  def getSize: Int

  def getInvalidationSize: (Int, Int)

  def resetStatistics(): Unit

  def put(entry: E): Unit

  def invalidate(invalidationKey: String): Unit = invalidate(invalidationKey, propagate = true)

  def invalidate(invalidationKey: String, propagate: Boolean): Unit

  def test(key: K): Boolean

  def getKeys: Seq[K]

  def get(key: K): Option[V]

  /** Return the cached value for `keys`, or compute one with the supplied provider. One is expected to return an entry
    * keyed on `keys`.
    */
  final def getOrCompute(compute: () => E, key: K): V =
    get(key).getOrElse(compute().tap(put).value)

  final def getValidOrCompute(compute: () => E, valid: V => Boolean, key: K): V =
    get(key).filter(valid).getOrElse(compute().tap(put).value)

  def remove(key: K): Unit = remove(key, propagate = true)

  def remove(key: K, propagate: Boolean): Unit

  def clear(): Unit
end Cache
