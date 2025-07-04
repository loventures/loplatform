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

import _root_.enumeratum.EnumEntry

final class EnumEntryOps[E <: EnumEntry](private val self: E) extends AnyVal:

  /** Summon the enum companion object for this enum entry.
    */
  @inline
  def enumeratum(implicit ee: misc.Enumerative[E]): ee.`enum`.type = ee.`enum`

trait ToEnumEntryOps:
  import language.implicitConversions

  @inline
  implicit final def ToEnumEntryOps[E <: EnumEntry](self: E): EnumEntryOps[E] =
    new EnumEntryOps[E](self)
