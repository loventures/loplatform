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

import scalaz.{Equal, MonadPlus, Unapply}
import scalaz.syntax.equal.*
import scaloi.Zero

/** Enhancements upon MonadPlus. */
final class MonadPlusOps[M[_], A](private val self: M[A]) extends AnyVal:

  /** Filter out zero values from `self`.
    *
    * @param M
    *   [[MonadPlus]] evidence for `M`
    * @param Z
    *   [[Zero]] evidence for `A`
    * @param E
    *   [[Equal]] evidence for `A`
    * @return
    *   `self' without the zeroes`
    */
  def filterNZ(implicit M: MonadPlus[M], Z: Zero[A], E: Equal[A]): M[A] =
    M.filter(self)(_ =/= Z.zero)
end MonadPlusOps

trait ToMonadPlusOps extends ToMonadPlusOps0:
  import language.implicitConversions

  @inline
  implicit final def ToMonadPlusOps[M[_]: MonadPlus, A](self: M[A]): MonadPlusOps[M, A] =
    new MonadPlusOps[M, A](self)

trait ToMonadPlusOps0:
  import language.implicitConversions

  @inline
  implicit final def ToMonadPlusOps0[MA](self: MA)(implicit UA: Unapply[MonadPlus, MA]): MonadPlusOps[UA.M, UA.A] =
    new MonadPlusOps[UA.M, UA.A](UA(self))
