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
package syntax

import scala.collection.Set
import scala.language.implicitConversions

/** Enhancements on sets.
  *
  * @param self
  *   the set
  * @tparam A
  *   the set value type
  */
final class SetOps[A](private val self: Set[A]) extends AnyVal:

  /** Convert this set to a map with a function from keys to values.
    *
    * @param f
    *   a function from keys to values
    * @tparam B
    *   the value type
    * @return
    *   the resulting map
    */
  def mapTo[B](f: A => B): Map[A, B] = self.map(a => a -> f(a)).toMap

  /** Test whether two sets intersect.
    * @param as
    *   the other set
    * @tparam AA
    *   the other set element type
    * @return
    *   whether the sets intersect
    */
  def intersects[AA <: A](as: Set[AA]): Boolean = as exists self.contains
end SetOps

/** Implicit conversion for set tag operations.
  */
trait ToSetOps:

  /** Implicit conversion from set to the set enhancements.
    * @param c
    *   the set
    * @tparam C
    *   its type
    */
  implicit def toSetOps[C](c: Set[C]): SetOps[C] = new SetOps(c)
