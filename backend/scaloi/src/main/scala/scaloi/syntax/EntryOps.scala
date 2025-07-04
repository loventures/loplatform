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

import java.util.Map.Entry

import scala.language.implicitConversions

/** Map entry pimping operations.
  *
  * @param self
  *   the entry to pimp
  * @tparam A
  *   the key type
  * @tparam B
  *   the value type
  */
final class EntryOps[A, B](private val self: Entry[A, B]) extends AnyVal:

  /** Convert a map entry to a scala tuple.
    *
    * @return
    *   a tuple
    */
  def asScala: (A, B) = self.getKey -> self.getValue

/** Implicit conversion for map entry operations.
  */
trait ToEntryOps:

  /** Implicit conversion from map entry to the entry enhancements.
    * @param e
    *   the entry
    * @tparam A
    *   the key type
    * @tparam B
    *   the value type
    */
  implicit def toEntryOps[A, B](e: Entry[A, B]): EntryOps[A, B] = new EntryOps(e)
end ToEntryOps
