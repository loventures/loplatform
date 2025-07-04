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

import scalaz.Monoid
import scalaz.std.set.*
import scalaz.syntax.std.option.*
import scaloi.syntax.OptionOps.*

import java.io.Serializable
import scala.collection.mutable

/** A time bucket with its expiration time (when it should no longer accept elements). */
private[cache] final class CacheBucket[K <: Serializable, V >: Null](expires: Long):

  @inline final def stillValid(when: Long): Boolean = expires > when

  private val entries         = mutable.Map.empty[K, Entry[K, V]]
  private val invalidationMap = mutable.Map.empty[String, mutable.Buffer[K]]

  def size: Int = entries.size

  def keys: Iterable[K] = entries.keys

  def values: Iterable[Entry[K, V]] = entries.values

  def invalidationSize: (Int, Int) = invalidationMap.size -> invalidationMap.values.map(_.size).sum

  def add(entry: Entry[K, V]): Option[Entry[K, V]] =
    val existing            = entries.put(entry.key, entry)
    val oldInvalidationKeys = existing.map(_.invalidationKeys).orZero
    val invalidationKeys    = entry.invalidationKeys
    (invalidationKeys &~ oldInvalidationKeys).foreach { inv =>
      invalidationMap.getOrElseUpdate(inv, mutable.Buffer.empty[K]) += entry.key
    }
    (oldInvalidationKeys &~ invalidationKeys).foreach { inv =>
      unlink(inv, entry.key)
    }
    existing
  end add

  def remove(key: K): Option[Entry[K, V]] =
    entries.remove(key) <|? { entry =>
      entry.invalidationKeys.foreach { inv =>
        unlink(inv, key)
      }
    }

  private def unlink(inv: String, key: K): Unit =
    invalidationMap.get(inv) foreach { keys =>
      if (keys -= key).isEmpty then invalidationMap.remove(inv)
    }

  def contains(key: K): Boolean =
    entries.contains(key)

  def invalidate(inv: String): Unit =
    invalidationMap.remove(inv) foreach { keys =>
      keys foreach remove
    }

  def get(key: K): Option[Entry[K, V]] =
    entries.get(key)
end CacheBucket

object CacheBucket:
  implicit def mSeq[A]: Monoid[Seq[A]] = Monoid.instance((a, b) => a ++ b, Seq.empty)
