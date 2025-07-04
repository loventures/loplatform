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
package misc

import java.lang as jl

/** Typeclass evidence of an isomorphism between a primitive number type and its boxing.
  *
  * Boxes[From, To] is defined only if To is the object type boxing From.
  *
  * @tparam From
  *   the unboxed type
  * @tparam To
  *   the boxed type
  */
sealed trait Boxes[From <: AnyVal, To]:
  def box(f: From): To
  def unbox(f: To): From

object Boxes:
  private case class Impl[From <: AnyVal, To >: Null](
    b: From => To,
    u: To => From
  ) extends Boxes[From, To]:
    def box(f: From) = b(f)
    def unbox(t: To) = u(t)

  implicit val boxesByte: Boxes[Byte, jl.Byte]          =
    Impl[Byte, jl.Byte](jl.Byte.valueOf, _.byteValue)
  implicit val boxesShort: Boxes[Short, jl.Short]       =
    Impl[Short, jl.Short](jl.Short.valueOf, _.shortValue)
  implicit val boxesInt: Boxes[Int, jl.Integer]         =
    Impl[Int, jl.Integer](jl.Integer.valueOf, _.intValue)
  implicit val boxesLong: Boxes[Long, jl.Long]          =
    Impl[Long, jl.Long](jl.Long.valueOf, _.longValue)
  implicit val boxesFloat: Boxes[Float, jl.Float]       =
    Impl[Float, jl.Float](jl.Float.valueOf, _.floatValue)
  implicit val boxesDouble: Boxes[Double, jl.Double]    =
    Impl[Double, jl.Double](jl.Double.valueOf, _.doubleValue)
  implicit val boxesBoolean: Boxes[Boolean, jl.Boolean] =
    Impl[Boolean, jl.Boolean](jl.Boolean.valueOf, _.booleanValue)
end Boxes
