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

import scalaz.Cobind

/** Enhancements on cobinds.
  *
  * @param self
  *   the cobind
  * @tparam F
  *   the cobind type
  * @tparam A
  *   the cobound type
  */
final class CobindOps[F[_], A](private val self: F[A]) extends AnyVal:

  /** Apply [self] to a side-effecting function, if applicable, discarding any result.
    *
    * @param f
    *   the side-effecting function
    * @param F
    *   cobind evidence
    * @tparam B
    *   the discarded result type
    */
  @inline
  final def coflatForeach[B](f: F[A] => B)(implicit F: Cobind[F]): Unit =
    F.cobind(self)(f)
end CobindOps

/** Implicit conversion for cobind operations.
  */
trait ToCobindOps:
  import language.implicitConversions

  /** Implicit conversion from cobind to the cobind enhancements.
    * @param f
    *   the cobind
    * @tparam F
    *   the cobind type
    * @tparam A
    *   the cobound type
    */
  @inline
  implicit final def toCobindOps[F[_]: Cobind, A](f: F[A]): CobindOps[F, A] = new CobindOps(f)
end ToCobindOps
