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

package clots.syntax

import cats.syntax.applicative.*
import cats.syntax.functor.*
import cats.{Applicative, ApplicativeError}

import scala.language.implicitConversions

trait ClotsBooleanSyntax:
  implicit final def clotsSyntaxBoolean(self: Boolean): ClotsBooleanOps = new ClotsBooleanOps(self)

final class ClotsBooleanOps(private val self: Boolean) extends AnyVal:

  /** If this boolean is `true` then return the supplied value, else return `Unit` lifted into the [[Applicative]]. */
  def whenA[F[_]: Applicative, A](fa: => F[A]): F[Unit] =
    if self then fa.void else ().pure[F]

  /** If this boolean is `false` then return the supplied value, else return `Unit` lifted into the [[Applicative]]. */
  def unlessA[F[_]: Applicative, A](fa: => F[A]): F[Unit] =
    if self then ().pure[F] else fa.void

  /** If this boolean is `true` then raise the error `e`, else return `F.unit`. */
  def raiseWhen[F[_], E](e: => E)(implicit F: ApplicativeError[F, ? >: E]): F[Unit] =
    F.raiseWhen(self)(e)

  /** Same as `raiseWhen` but with improved English */
  def thenRaise[F[_], E](e: => E)(implicit F: ApplicativeError[F, ? >: E]): F[Unit] = raiseWhen(e)

  /** If this boolean is `false` then raise the error `e`, else return `F.unit`. */
  def raiseUnless[F[_], E](e: => E)(implicit F: ApplicativeError[F, ? >: E]): F[Unit] =
    F.raiseUnless(self)(e)

  /** Same as `raiseUnless` but with improved English */
  def elseRaise[F[_], E](e: => E)(implicit F: ApplicativeError[F, ? >: E]): F[Unit] = raiseUnless(e)
end ClotsBooleanOps
