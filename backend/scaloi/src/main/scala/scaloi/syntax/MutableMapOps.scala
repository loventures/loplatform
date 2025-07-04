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

import scala.collection.mutable.Map as MutableMap
import scala.language.implicitConversions
import scalaz.{Monoid, Semigroup}

/** Enhancements on mutable maps.
  * @param self
  *   the mutable map
  * @tparam A
  *   the key type
  * @tparam B
  *   the value type
  */
final class MutableMapOps[A, B](private val self: MutableMap[A, B]) extends AnyVal:

  /** Return a mutable map with a default value of the monoidal zero.
    * @param ev
    *   monoid evidence for the value type
    * @return
    *   the mutable map
    */
  @inline final def withDefaultZero(implicit ev: Monoid[B]): MutableMap[A, B] = self.withDefaultValue(ev.zero)

  /** Get a value from the map, if present, or else update with the monoidal zero.
    * @param a
    *   the key
    * @param ev
    *   monoid evidence for the value type
    * @return
    *   the resulting value
    */
  @inline final def getOrElseUpdateZ(a: A)(implicit ev: Monoid[B]): B = self.getOrElseUpdate(a, ev.zero)

  /** Append a value to this map. If there is an existing value under the chosen key then it is appended with the new
    * value, else it is stored directly.
    * @param a
    *   the key
    * @param b
    *   the value
    * @param ev
    *   semigroup evidence for the value type
    */
  @inline final def append(a: A, b: B)(implicit ev: Semigroup[B]): Unit =
    self.update(a, self.get(a).fold(b)(ev.append(_, b)))
end MutableMapOps

/** Implicit conversion for mutable map operations.
  */
trait ToMutableMapOps:

  /** Implicit conversion from mutable map to the mutable map enhancements.
    * @param m
    *   the mutable map
    * @tparam A
    *   the key type
    * @tparam B
    *   the value type
    */
  implicit def toMutableMapOps[A, B](m: MutableMap[A, B]): MutableMapOps[A, B] =
    new MutableMapOps(m)
end ToMutableMapOps
