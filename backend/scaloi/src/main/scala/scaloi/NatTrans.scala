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

/** Some general Natural transformations
  */
object NatTrans {
  /* scala 3
import scalaz.{Coproduct, Free, Monad, ~>}

import scala.collection.mutable


  /** This should work with foldMap, need some way to prove `List[F[A]]` is a monad.
   */
  def aggregate[F[_]] = new (F ~> Lambda[A => List[F[A]]]) {
    override def apply[A](fa: F[A]): List[F[A]] = List(fa)
  }

  /** Interpret a Free[F, A] into a H[A] with the transformation F[A] ~> H[A].
   * @see
   *   scalaz.Free#flatMapSuspension
   */
  def freeIntp[F[_], H[_]: Monad](intp: F ~> H): (Free[F, *] ~> H) = new (Free[F, *] ~> H) {
    override def apply[A](fa: Free[F, A]): H[A] = fa.foldMap(intp)
  }

  /** Append instances of fa to mutable list.
   */
  class MutableRecorder[F[_]] extends (F ~> F) {
    val ops                               = mutable.Buffer.empty[F[_]]
    override def apply[A](fa: F[A]): F[A] = {
      ops += fa
      fa
    }
  }

  /** A few extension methods on natural transformations.
   */
  implicit class NatTransFunctions[F[_], G[_]](self: (F ~> G)) {
    // (F ~> G) or (H ~> G) = (Coproduct(F,H) ~> G)
    def or[H[_]](f: H ~> G): Coproduct[F, H, *] ~> G = Or(self, f)
  }

  case class Or[F[_], G[_], H[_]](fh: F ~> H, gh: G ~> H) extends (Coproduct[F, G, *] ~> H) {
    override def apply[A](c: Coproduct[F, G, A]): H[A] = c.run.fold(fh.apply, gh.apply)
  }
   */
}
