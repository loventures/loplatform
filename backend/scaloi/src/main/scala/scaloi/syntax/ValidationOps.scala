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

package scaloi.syntax

import scalaz.{Validation, ValidationNel}

import scala.language.implicitConversions
import scala.util.Try

final class ValidationOps[E, X](private val self: Validation[E, X]) extends AnyVal:

  @inline def toTry(f: E => Throwable): Try[X] =
    self match
      case scalaz.Failure(e) => scala.util.Failure(f(e))
      case scalaz.Success(x) => scala.util.Success(x)

  @inline def leftTap[F](f: E => F): Validation[E, X] = self.leftMap(e =>
    f(e); e
  )
end ValidationOps

final class ValidationAnyOps[A](private val self: A) extends AnyVal:
  import boolean.*

  /** Return this as a validation success if a predicate passes, else a supplied validation failure.
    * @param f
    *   the predicate
    * @param e
    *   the failure
    * @tparam E
    *   the failure type
    * @return
    *   the resulting [[Validation]]
    */
  def validWhen[E](f: A => Boolean, e: => E): Validation[E, A] = f(self).elseInvalid(e, self)

  /** Return a supplied validation failure if a predicate passes, else this as a validation success.
    * @param f
    *   the predicate
    * @param e
    *   the failure
    * @tparam E
    *   the failure type
    * @return
    *   the resulting [[Validation]]
    */
  def validUnless[E](f: A => Boolean, e: => E): Validation[E, A] = f(self).thenInvalid(e, self)

  /** Return this as a validation success if a predicate passes, else a supplied validation failure in a non-empty list.
    * @param f
    *   the predicate
    * @param e
    *   the failure
    * @tparam E
    *   the failure type
    * @return
    *   the resulting [[ValidationNel]]
    */
  def validNelWhen[E](f: A => Boolean, e: => E): ValidationNel[E, A] = f(self).elseInvalidNel(e, self)

  /** Return a supplied validation failure in a non-empty list if a predicate passes, else this as a validation success.
    * @param f
    *   the predicate
    * @param e
    *   the failure
    * @tparam E
    *   the failure type
    * @return
    *   the resulting [[ValidationNel]]
    */
  def validNelUnless[E](f: A => Boolean, e: => E): ValidationNel[E, A] = f(self).thenInvalidNel(e, self)
end ValidationAnyOps

/** Implicit conversion for Validation operations.
  */
trait ToValidationOps extends Any:

  implicit def toValidationOps[E, X](v: Validation[E, X]): ValidationOps[E, X] = new ValidationOps(v)
  implicit def toValidationAnyOps[A](a: A): ValidationAnyOps[A]                = new ValidationAnyOps(a)
