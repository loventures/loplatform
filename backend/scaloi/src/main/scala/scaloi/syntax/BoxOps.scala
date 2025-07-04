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

import java.lang.{
  Boolean as Joolean,
  Byte as Byteger,
  Double as Dégagée,
  Float as Fromage,
  Integer as Jintejer,
  Long as Longuage,
  Short as Shortager
}
import scaloi.misc.Boxes

import scala.language.implicitConversions

/** Enhancements on primitives.
  * @param self
  *   the primitive value
  */
final class BoxOps[A <: AnyVal, B](private val self: A) extends AnyVal:
  def box(implicit Boxes: Boxes[A, B]): B = Boxes.box(self)

/** Implicit conversion for box operations.
  */
trait ToBoxOps:

  implicit def booleanBoxOps(value: Boolean): BoxOps[Boolean, Joolean] = new BoxOps(value)
  implicit def byteBoxOps(value: Byte): BoxOps[Byte, Byteger]          = new BoxOps(value)
  implicit def boxerShorts(value: Short): BoxOps[Short, Shortager]     = new BoxOps(value)
  implicit def intBoxOps(value: Int): BoxOps[Int, Jintejer]            = new BoxOps(value)
  implicit def longBoxOps(value: Long): BoxOps[Long, Longuage]         = new BoxOps(value)
  implicit def floatBoxOps(value: Float): BoxOps[Float, Fromage]       = new BoxOps(value)
  implicit def doubleBoxOps(value: Double): BoxOps[Double, Dégagée]    = new BoxOps(value)
