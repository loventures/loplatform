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

import scalaz.Functor

/** Enhancements on functors.
  *
  * @param self
  *   the functor
  * @tparam F
  *   the functor type
  * @tparam A
  *   the functed type
  */
final class FunctorOps[F[_], A](private val self: F[A]) extends AnyVal:

  /** Apply a partial function to the values in this functor. Values for which the partial function is undefined remain
    * unchanged.
    * @param pf
    *   the partial function
    * @param F
    *   the functor evidence
    * @tparam A1
    *   the result type
    * @return
    *   the partially transformed functor
    */
  @inline
  final def pfMap[A1 >: A](pf: PartialFunction[A, A1])(implicit F: Functor[F]): F[A1] =
    F.map(self)(fa => pf.applyOrElse(fa, (a: A) => a))

  /** Inject `b` to the right of the [[A]]s in `self`.
    * @param b
    *   the other value
    * @tparam B
    *   the content type
    * @return
    *   the associated values
    */
  def <*-[B](b: B)(implicit F: Functor[F]): F[(A, B)] = F.strengthR(self, b)

  /** Inject `b` to the left of the [[A]]s in `self`.
    *
    * @param b
    *   the other value
    * @tparam B
    *   the content type
    * @return
    *   the associated values
    */
  def -*>:[B](b: B)(implicit F: Functor[F]): F[(B, A)] = F.strengthL(b, self)
end FunctorOps

/** Implicit conversion for functor operations.
  */
trait ToFunctorOps:
  import language.implicitConversions

  /** Implicit conversion from functor to the functor enhancements.
    * @param f
    *   the functor
    * @tparam F
    *   the functor type
    * @tparam A
    *   the functed type
    */
  @inline
  implicit final def toFunctorOps[F[_]: Functor, A](f: F[A]): FunctorOps[F, A] = new FunctorOps(f)
end ToFunctorOps
