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

import scalaz.{Monad, Unapply}

/** Enhancements upon Monads. */
final class MonadOps[M[_], A](private val self: M[A]) extends AnyVal:

  /** Compute an effect from the contained value, then apply that effect to `self`, discarding the computed value.
    *
    * @param f
    *   the effectful function
    * @param M
    *   evidence of the monadicity of `M`
    * @return
    *   `self` with the effects of `f(self)` applied
    */
  def flatTap[B](f: A => M[B])(implicit M: Monad[M]): M[A] =
    M.bind(self)(a => M.map(f(a))(_ => a))
end MonadOps

trait ToMonadOps extends ToMonadOps0:
  import language.implicitConversions

  @inline
  implicit final def ToMonadOps[M[_]: Monad, A](self: M[A]): MonadOps[M, A] =
    new MonadOps[M, A](self)

trait ToMonadOps0:
  import language.implicitConversions

  @inline
  implicit final def ToMonadOps0[MA](self: MA)(implicit UA: Unapply[Monad, MA]): MonadOps[UA.M, UA.A] =
    new MonadOps[UA.M, UA.A](UA(self))
