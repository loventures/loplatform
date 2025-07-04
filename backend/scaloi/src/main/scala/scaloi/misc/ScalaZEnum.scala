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

package scaloi.misc

import enumeratum.*
import scalaz.{Ordering, Enum as Znum}

/** Mixin to provide scalaz Enum and Order typeclass evidence for enumeratum enums based on their ordinal index.
  *
  * Usage:
  * {{{
  *   sealed trait Greeting extends EnumEntry
  *
  *   object Greeting extends Enum[Greeting] with ScalaZEnum[Greeting] {
  *     val values = findValues
  *     case object Hi extends Greeting
  *     case object Hello extends Greeting
  *   }
  *
  *   // Greeting.values.maximum == Some(Greeting.Hello)
  * }}}
  *
  * @tparam A
  *   the enumeration type
  */
trait ScalaZEnum[A <: EnumEntry]:
  self: Enum[A] =>

  implicit object EnumZnum extends Znum[A]:
    private final val n = self.values.size

    override def pred(a: A): A               = self.values((self.indexOf(a) + n - 1) % n)
    override def succ(a: A): A               = self.values((self.indexOf(a) + 1) % n)
    override def min: Option[A]              = self.values.headOption
    override def max: Option[A]              = self.values.lastOption
    override def order(x: A, y: A): Ordering =
      Ordering.fromInt(self.indexOf(x) - self.indexOf(y))
end ScalaZEnum
