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

package loi.cp

import scalaz.\/
import scalaz.syntax.bifunctor.*
import scala.language.implicitConversions

/** Trait describing a type that can be widened to a common supertype [T].
  * @tparam T
  *   the supertype itself
  */
trait Widen[T]:
  self: T =>

  /** Widen a subclass to the superclass. */
  def widen: T = self

/** Widening companion.
  */
object Widen:

  /** Implicit operations on disjunctions with widenable lefts. */
  implicit class WidenErrorOps[A, B](private val ab: A \/ B) extends AnyVal:

    /** Widen the left side of this disjunction to the common supertype.
      * @param ev
      *   Evidence that the left type is widenable
      * @tparam AA
      *   the common supertype
      * @return
      *   the disjunction with the widened left type
      */
    def widenl[AA](implicit ev: A <:< Widen[AA]): AA \/ B = ab.leftMap(ev(_).widen)
  end WidenErrorOps

  /** Implicit evidence that a disjunction is really covariant. */
  implicit def disjunctivariance[A, B, AA >: A, BB >: B](ab: A \/ B): AA \/ BB = ab.widen[AA, BB]

  /** Implicit evidence that a disjunction with nothing on the right is really covariant. */
  implicit def leftOfNothingDisjunctivariance[A, B, AA >: A, BB](ab: A \/ Nothing): AA \/ BB = ab.widen[AA, BB]
end Widen
