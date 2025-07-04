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

import scalaz.{Foldable, MonadPlus}
import scalaz.Id.Id

import scala.collection.IterableOps

/** Maps flatter than flat. */
trait Hypermonad[F[_], G[_], H[_]]:

  /** Given a container of [[A]] and a function from [[A]] to a container of containers of [[B]], return just a
    * container of bees.
    */
  def flatterMap[A, B](fa: F[A], f: A => G[H[B]]): F[B]

object Hypermonad extends LowPriHypermonad:
  implicit def gtHypermonad[F[X] <: IterableOnce[X] & IterableOps[X, F, F[X]], G[_]: Foreach, H[_]: Foreach]
    : Hypermonad[F, G, H] = new Hypermonad[F, G, H]:
    override def flatterMap[A, B](fa: F[A], f: A => G[H[B]]): F[B] =
      val ForeachGH = Foreach[G].compose(using Foreach[H])
      val builder   = fa.iterableFactory.newBuilder[B]
      fa.foreach((a: A) =>
        ForeachGH.foreach(f(a)) { b =>
          builder += b
        }
      )
      builder.result()
end Hypermonad

trait LowPriHypermonad:
  implicit def foldableHypermonad[F[_]: MonadPlus, G[_]: Foldable, H[_]: Foldable]: Hypermonad[F, G, H] =
    new Hypermonad[F, G, H]:
      override def flatterMap[A, B](fa: F[A], f: A => G[H[B]]): F[B] =
        val FoldGH = Foldable[G].compose(using Foldable[H])
        MonadPlus[F].bind(fa) { a =>
          FoldGH.foldRight(f(a), MonadPlus[F].empty[B]) { (b, fb) =>
            MonadPlus[F].plus(MonadPlus[F].point(b), fb)
          }
        }

  implicit def weakFoldableHypermonad[F[_]: MonadPlus, G[_]: Foldable]: Hypermonad[F, G, Id] =
    new Hypermonad[F, G, Id]:
      override def flatterMap[A, B](fa: F[A], f: A => G[Id[B]]): F[B] =
        MonadPlus[F].bind(fa) { a =>
          Foldable[G].foldRight(f(a), MonadPlus[F].empty[B]) { (b, fb) =>
            MonadPlus[F].plus(MonadPlus[F].point(b), fb)
          }
        }
end LowPriHypermonad
