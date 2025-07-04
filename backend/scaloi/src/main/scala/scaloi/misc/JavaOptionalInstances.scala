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

import java.util as ju

import scalaz.{Alt, Applicative, MonadPlus, Optional, Traverse, \/}
import scalaz.Isomorphism.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*

/** ju.Optional is a passable scalaz.Optional, scalaz.MonadPlus. */
trait JavaOptionalInstances:

  implicit final val javaOptionalInstance
    : MonadPlus[ju.Optional] & Alt[ju.Optional] & Optional[ju.Optional] & Traverse[ju.Optional] =
    new MonadPlus[ju.Optional] with Alt[ju.Optional] with Optional[ju.Optional] with Traverse[ju.Optional]:
      def empty[A]: ju.Optional[A] = ju.Optional.empty[A]

      def plus[A](a: ju.Optional[A], b: => ju.Optional[A]): ju.Optional[A] =
        if a.isPresent then a else b

      def point[A](a: => A): ju.Optional[A] = ju.Optional.of(a)

      def bind[A, B](fa: ju.Optional[A])(f: A => ju.Optional[B]): ju.Optional[B] =
        fa `flatMap` f.apply

      def pextract[B, A](fa: ju.Optional[A]): ju.Optional[B] \/ A =
        fa.map[ju.Optional[B] \/ A](_.right) `orElse` ju.Optional.empty[B].left

      def traverseImpl[F[_], A, B](fa: ju.Optional[A])(f: A => F[B])(implicit F: Applicative[F]): F[ju.Optional[B]] =
        fa.map[F[ju.Optional[B]]](a => F.map(f(a))(ju.Optional.of[B])) `orElse` F.point(ju.Optional.empty[B])

      def alt[A](a1: => ju.Optional[A], a2: => ju.Optional[A]): ju.Optional[A] = if a1.isPresent then a1 else a2

  implicit val optionJavaOptionalIso: Option <~> ju.Optional =
    new IsoFunctorTemplate[Option, ju.Optional]:
      def to[A](fa: Option[A]): ju.Optional[A]   = fa.cata(ju.Optional.of[A], ju.Optional.empty[A])
      def from[A](ga: ju.Optional[A]): Option[A] = ga.map[Option[A]](Option.apply(_)).orElse(Option.empty)

  implicit val javaOptionalOptionIso: ju.Optional <~> Option = optionJavaOptionalIso.flip // why?
end JavaOptionalInstances

object JavaOptionalInstances extends JavaOptionalInstances
