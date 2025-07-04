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

package scaloi.misc

import scalaz.Foldable
import scalaz.Id.Id

import scala.collection.IterableOnce

/** Typeclass evidence for the ability to side-effectively iterate over a container type. */
trait Foreach[F[_]]:
  self =>
  def foreach[A, U](fa: F[A])(f: A => U): Unit

  /** Composition of foreach over nested types. */
  final def compose[G[_]](implicit G: Foreach[G]): Foreach[([X] =>> F[G[X]])] = new Foreach:
    override def foreach[A, U](fa: F[G[A]])(f: A => U): Unit = self.foreach(fa)(ga => G.foreach(ga)(f))

object Foreach extends LowPriForeach:
  def apply[F[_]](implicit ev: Foreach[F]): Foreach[F] = ev

  /** Foreach evidence of [[Option]]. */
  implicit def optionForeach: Foreach[Option] = new Foreach[Option]:
    override def foreach[A, U](fa: Option[A])(f: A => U): Unit = fa.foreach(f)

  /** Foreach evidence of [[GenTraversableOnce]]. */
  implicit def gt1Foreach[F[X] <: IterableOnce[X]]: Foreach[F] = new Foreach[F]:
    override def foreach[A, U](fa: F[A])(f: A => U): Unit = fa.iterator.foreach(f)

  /** Foreach evidence of [[Id]]. */
  implicit def idForeach: Foreach[Id] = new Foreach[Id]:
    override def foreach[A, U](fa: Id[A])(f: A => U): Unit = f(fa)
end Foreach

trait LowPriForeach:

  /** Foreach evidence of a type with [[Foldable]] evidence. */
  implicit def foldableForeach[F[_]: Foldable]: Foreach[F] = new Foreach[F]:
    override def foreach[A, U](fa: F[A])(f: A => U): Unit = Foldable[F].foldLeft(fa, ()) { case (_, a) =>
      f(a)
    }
