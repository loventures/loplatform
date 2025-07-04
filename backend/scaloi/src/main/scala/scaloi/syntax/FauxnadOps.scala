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

import scalaz.Functor

import scala.language.implicitConversions

/** Pretends to scala that IStream, IO etc support `withFilter` so they can be used with tuple desugaring. A replacement
  * for https://github.com/oleg-py/better-monadic-for in preparation for Scala 3. The Scala 3 `-source:future` flag is
  * considered a step too far. Considered dangerous because you might expect withFilter to work in some cases.
  */
final class FauxnadOps[F[_], A](private val self: F[A]) extends AnyVal:
  def withFilter(f: A => Boolean)(implicit F: Functor[F]): F[A] =
    F.map(self) { a =>
      assert(f(a), "Fauxnad filter shall pass")
      a
    }

trait ToFauxnadAnyOps:

  /** This operates on any F because it is used for cats things that are beneath scaloi's contempt. */
  implicit def toFauxnad[F[_], A](fa: F[A]): FauxnadOps[F, A] = new FauxnadOps(fa)
