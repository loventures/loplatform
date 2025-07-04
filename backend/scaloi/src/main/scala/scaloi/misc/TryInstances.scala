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
package misc

import scalaz.std.list.*
import scalaz.syntax.foldable.*
import scalaz.{Failure as _, Success as _, *}

import scala.util.*

trait TryInstances:
  implicit final val tryInstance: Monad[Try] & Traverse[Try] =
    new Monad[Try] with Traverse[Try]:
      override def point[A](a: => A): Try[A]                                                      = Try(a)
      override def ap[A, B](fa: => Try[A])(f: => Try[A => B]): Try[B]                             = fa.flatMap(a => f.map(x => x(a)))
      override def bind[A, B](fa: Try[A])(f: A => Try[B])                                         = fa.flatMap(f)
      override def traverseImpl[G[_], A, B](fa: Try[A])(f: A => G[B])(implicit G: Applicative[G]) =
        fa match
          case Success(a)   => G.map(f(a))(Success(_))
          case Failure(err) => G.point(Failure(err))

  implicit final def tryEqual[A](implicit A: Equal[A], throwable: Equal[Throwable]): Equal[Try[A]] =
    new Equal[Try[A]]:
      def equal(ta: Try[A], tb: Try[A]) = PartialFunction.cond((ta, tb)) {
        case (Success(a), Success(b)) => A.equal(a, b)
        case (Failure(a), Failure(b)) => throwable.equal(a, b)
      }

  def tryListAppend[A](fas: Try[List[A]], fbs: => Try[List[A]]): Try[List[A]] =
    fas.flatMap(as => fbs.map(bs => as ::: bs))

  implicit def tryListMonoid[A]: Monoid[Try[List[A]]] =
    Monoid.instance(tryListAppend, Success(Nil))

  implicit class TryIterableOps[A](as: List[A]):
    def traverseTryListFF[B](f: A => Try[B]) = as.foldMap(a => f(a).map(b => List(b)))
end TryInstances

object TryInstances extends TryInstances:
  import Isomorphism.*

  type ThrowableDisjunction[X] = Throwable \/ X

  val tryIsoDisjunction: Try <~> ThrowableDisjunction =
    new IsoFunctorTemplate[Try, ThrowableDisjunction]:
      import scalaz.syntax.std.`try`.*
      def to[A](fa: Try[A])           = fa.toDisjunction
      def from[A](ga: Throwable \/ A) = ga.fold(Failure(_), Success(_))
end TryInstances
