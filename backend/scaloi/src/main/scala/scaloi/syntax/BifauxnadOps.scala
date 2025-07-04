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

import scalaz.{Bifunctor, Functor, Validation, \/}

import scala.language.implicitConversions

/** Pretends to scala that \/ etc support `withFilter` so they can be used with tuple desugaring. A replacement for
  * https://github.com/oleg-py/better-monadic-for in preparation for Scala 3. The Scala 3 `-source:future` flag is
  * considered a step too far. If you actually think you can filter these types you are sorely mistaken.
  */
trait ToBifauxnadOps:
  implicit def toBifauxnadDisjunction[A, B](ab: A \/ B): FauxnadOps[[X] =>> A \/ X, B]                    = new FauxnadOps(ab)
  implicit def toBifauxnadEither[A, B](ab: Either[A, B]): FauxnadOps[[X] =>> Either[A, X], B]             = new FauxnadOps(ab)
  implicit def toBifauxnadValidation[A, B](ab: Validation[A, B]): FauxnadOps[[X] =>> Validation[A, X], B] =
    new FauxnadOps(ab)

  /** A Functor over the right hand side of a bifunctor. */
  implicit def bifunctorRifunctor[F[_, _]: Bifunctor, A]: Functor[[X] =>> F[A, X]] = new Functor[[X] =>> F[A, X]]:
    override def map[B, C](fab: F[A, B])(f: B => C): F[A, C] = Bifunctor[F].rightMap(fab)(f)

trait ToBifauxnadAnyOps:

  /** Bifauxnad anything. */
  implicit def toBifauxnad[F[_, _], A, B](ab: F[A, B]): FauxnadOps[[X] =>> F[A, X], B] = new FauxnadOps(ab)
