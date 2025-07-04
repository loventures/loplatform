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

import scala.language.implicitConversions

/** Enhancements on zeroes.
  *
  * @param self
  *   the zero
  * @tparam A
  *   the zero type
  */
final class ZeroOps[A](private val self: A) extends AnyVal:

  /** Test whether `self` is zero. */
  def isZero(implicit Z: Zero[A]): Boolean = Z.isZero(self)

  /** Test whether `self` is non-zero. */
  def nonZero(implicit Z: Zero[A]): Boolean = Z.nonZero(self)

  /** Zero out nulls. */
  def zNull(implicit Z: Zero[A], ev: Null <:< A): A = if self == ev(null) then Z.zero else self

  /** This or that if this is zero.
    */
  def |||(o: => A)(implicit Z: Zero[A]): A = if Z.isZero(self) then o else self
end ZeroOps

/** Implicit conversion for zero operations.
  */
trait ToZeroOps:

  /** Implicit conversion from zero to the zero enhancements.
    * @param a
    *   the zero
    * @tparam A
    *   its type
    */
  implicit def toZeroOps[A: Zero](a: A): ZeroOps[A] = new ZeroOps(a)
