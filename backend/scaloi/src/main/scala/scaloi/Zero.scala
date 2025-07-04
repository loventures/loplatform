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

import scalaz.{Equal, IsEmpty, Monoid}

/** The unlawful ancestor of [[Monoid]]. */
trait Zero[A]:

  /** Get the zero value of the [[A]] type. */
  def zero: A

  /** Test whether an [[A]] is zero. */
  def isZero(a: A): Boolean

  /** Test whether an [[A]] is non-zero. */
  final def nonZero(a: A): Boolean = !isZero(a)
end Zero

object Zero extends ZeroInstances0:

  /** Summon the implicit zero evidence of a type [[A]]. */
  def apply[A](implicit ev: Zero[A]): Zero[A] = ev

  /** Define zero evidence for a type [[A]]. */
  def instance[A](z: A, isZ: A => Boolean): Zero[A] = new Zero[A]:
    override def zero: A               = z
    override def isZero(a: A): Boolean = isZ(a)

  /** Summon the zero value for a type [[A]]. */
  def zero[A](implicit ev: Zero[A]): A = ev.zero

  /** Unitary zero. */
  implicit def unitZero: Zero[Unit] = instance((), _ => true)

  /** Booleanic zero. */
  implicit def booleanZero: Zero[Boolean] = instance(false, !_)
end Zero

trait ZeroInstances0 extends ZeroInstances1:

  /** Zero evidence for an izempty. */
  implicit def derivedZeroF[F[_]: IsEmpty, A]: Zero[F[A]] = new Zero[F[A]]:
    override def zero: F[A]                = IsEmpty[F].empty
    override def isZero(fa: F[A]): Boolean = IsEmpty[F].isEmpty(fa)

trait ZeroInstances1:

  /** Zero evidence for a monoidal and equal type. */
  implicit def derivedZero[A: Equal: Monoid]: Zero[A] = new Zero[A]:
    override def zero: A               = Monoid[A].zero
    override def isZero(a: A): Boolean = Equal[A].equal(a, Monoid[A].zero)
