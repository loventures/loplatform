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

import java.lang as jl

/** Enhancements on [[Double]]s. */
final class DoubleOps(private val self: Double) extends AnyVal:
  import double.*
  import jl.Double.*

  /** Check whether this [[Double]] is within [[ε]] of `other`. */
  def ≈(other: Double): Boolean =
    longBitsToDouble(doubleToRawLongBits(self - other) & SignMasque) < ε

  /** Check whether this [[Double]] is less than `other`, within [[ε]]. */
  def ⪅(other: Double): Boolean = self < other || (this ≈ other)

  /** An alias for [[⪅]]. */
  @inline def <≈(other: Double): Boolean = ⪅(other)

  /** Check whether this [[Double]] is greater than `other`, within [[ε]]. */
  def ⪆(other: Double): Boolean = self > other || (this ≈ other)

  /** An alias for [[⪆]]. */
  @inline def >≈(other: Double): Boolean = ⪆(other)
end DoubleOps

trait DoubleVals:
  final val ε                          = 0.0001d // capriciously chosen
  private[syntax] final val SignMasque = 0x7fffffffffffffffL

trait ToDoubleOps:
  import language.implicitConversions

  @inline
  implicit final def ToDoubleOps(self: Double): DoubleOps = new DoubleOps(self)
