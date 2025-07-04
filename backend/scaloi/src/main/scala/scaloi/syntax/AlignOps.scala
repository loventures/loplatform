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

import scalaz.{Align, Monoid, \&/ as These}

/** Operations on [[Align]]able things.
  */
final class AlignOps[F[_], A](private val fa: F[A]) extends AnyVal:
  import These.{This, That, Both}

  /** Align this and `fb`, using `a` and `b` as default values for those types.
    *
    * @param fb
    *   the other [[F]] to align this with
    * @param a
    *   the value to pad with on the right
    * @param b
    *   the value to pad with on the left
    * @return
    *   an aligned `F[(A, B)]`
    */
  // noinspection VariablePatternShadow (it looks nice this way)
  def zipWithDefault[B](fb: F[B])(a: => A, b: => B)(implicit F: Align[F]): F[(A, B)] =
    F.alignWith[A, B, (A, B)] { // scalaz pls
      case This(a)    => (a, b)
      case That(b)    => (a, b)
      case Both(a, b) => (a, b)
    }(fa, fb)

  /** Align this and `fb`, using monoidal defaults for `A` and `B`.
    *
    * @param fb
    *   the other [[F]] to align this with
    * @return
    *   an aligned `F[(A, B)]`
    */
  def zipM[B](fb: F[B])(implicit F: Align[F], A: Monoid[A], B: Monoid[B]): F[(A, B)] =
    zipWithDefault(fb)(A.zero, B.zero)(using F)
end AlignOps

/** Implicit conversions to [[AlignOps]]. */
trait ToAlignOps:
  import language.implicitConversions

  /** Implicitly convert to [[AlignOps]]. */
  // XXX: F: Align constraint removed as it baffles intellij
  @inline implicit final def ToAlignOps[F[_] /*: Align*/, A](fa: F[A]): AlignOps[F, A] =
    new AlignOps[F, A](fa)
