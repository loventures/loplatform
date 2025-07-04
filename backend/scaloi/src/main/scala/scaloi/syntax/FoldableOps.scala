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
import scalaz.{Foldable, Monoid}

/** Enhancements on foldable things.
  * @param self
  *   the foldable thing
  * @tparam F
  *   the foldable type
  * @tparam A
  *   the folded type
  */
final class FoldableOps[F[_], A](private val self: F[A]) extends AnyVal:

  /** Apply a map to an optional value to the elements of this foldable, returning the first defined result.
    *
    * @param f
    *   the map function
    * @param ev
    *   foldable evidence
    * @tparam B
    *   the target type
    * @return
    *   the optional value
    */
  @inline final def findMap[B](f: A => Option[B])(implicit ev: Foldable[F]): Option[B] =
    ev.findMapM[scalaz.Id.Id, A, B](self)(f)

  /** Fold a foldable's worth of elements into another foldable's worth of monoidal values, then combine those values
    * monoidally.
    */
  @inline final def flatFoldMap[G[_], B](f: A => G[B])(implicit
    F: Foldable[F],
    G: Foldable[G],
    B: Monoid[B]
  ): B =
    F.foldMap(self)(a => G.fold(f(a)))

  /** Fold this to a [[Map]] after mapping each element to a tuple. Right bias for dups. */
  @inline final def foldToMap[B, C](f: A => (B, C))(implicit ev: Foldable[F]): Map[B, C] =
    ev.foldLeft(self, Map.empty[B, C])((bcs, a) => bcs + f(a))

  /** Convert a nested [[Foldable]] into nested [[List]] instances. */
  @inline final def hyperList[B](implicit ev: Foldable[F], x: A <:< F[B]): List[List[B]] =
    Foldable[F].toList(self).map(a => Foldable[F].toList(x(a)))
end FoldableOps

/** Implicit conversion for foldable operations.
  */
trait ToFoldableOps:

  /** Implicit conversion from foldable to its enhancements.
    * @param f:
    *   the foldable thing
    * @tparam F
    *   the foldable type
    * @tparam A
    *   the folded type
    */
  implicit def toFoldableOps[F[_]: Foldable, A](f: F[A]): FoldableOps[F, A] =
    new FoldableOps(f)
end ToFoldableOps
