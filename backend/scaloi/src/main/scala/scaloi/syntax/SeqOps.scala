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

/** Enhancements on seqs.
  *
  * @param self
  *   the seq
  * @tparam A
  *   the seq value type
  */
final class SeqOps[A](private val self: Seq[A]) extends AnyVal:

  /** Returns whether this seq has a given size.
    * @param size
    *   the size against which to test
    * @return
    *   whether this seq has that size
    */
  def hasSize(size: Int): Boolean = self.lengthCompare(size) == 0

  /** Convert each entry into a mapping from the entry index to the entry.
    *
    * @return
    *   Mapping from sequence index to value.
    */
  def mapByIndex: Map[Int, A] = self.zipWithIndex.map(_.swap).toMap
end SeqOps

/** Implicit conversion for seq tag operations.
  */
trait ToSeqOps:

  /** Implicit conversion from seq to the seq enhancements.
    * @param c
    *   the seq
    * @tparam C
    *   its type
    */
  implicit def toSeqOps[C](c: Seq[C]): SeqOps[C] = new SeqOps(c)
