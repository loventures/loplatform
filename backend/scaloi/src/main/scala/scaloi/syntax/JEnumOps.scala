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

import java.lang.Enum as JEnum

import scala.language.implicitConversions

/** Enhancements on java enums.
  *
  * @param self
  *   the enumeration value
  * @tparam A
  *   the enumeration type
  */
final class JEnumOps[A <: JEnum[A]](private val self: A) extends AnyVal:

  /** Compare enums by ordinal value.
    *
    * @param b
    *   the other enum value
    * @return
    *   if this enum value is less
    */
  @inline final def <(b: A): Boolean = self.ordinal < b.ordinal

  /** Compare enums by ordinal value.
    *
    * @param b
    *   the other enum value
    * @return
    *   if this enum value is less
    */
  @inline final def >(b: A): Boolean = self.ordinal > b.ordinal
end JEnumOps

/** Implicit conversion for class tag operations.
  */
trait ToJEnumOps:

  /** Implicit conversion from enum to the enum enhancements.
    * @param e
    *   the enum value
    * @tparam A
    *   its type
    */
  implicit def toJEnumOps[A <: JEnum[A]](e: A): JEnumOps[A] = new JEnumOps(e)
