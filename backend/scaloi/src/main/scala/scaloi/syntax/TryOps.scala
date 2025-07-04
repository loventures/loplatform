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

import scalaz.{NonEmptyList, ValidationNel, \/}
import scalaz.syntax.std.`try`.*

import scala.util.{Failure, Success, Try}

/** Enhancements on `Try`s. */
final class TryOps[T](private val self: Try[T]) extends AnyVal:
  import \/.{left, right}

  /** Transform matching failures with the provided partial function.
    *
    * @param fn
    *   the partial function with which to transform exceptions
    * @return
    *   `self` if successful, otherwise [[Failure]] of the error, transformed by `fn` if possible.
    */
  def mapExceptions(fn: PartialFunction[Throwable, Throwable]): Try[T] =
    self match
      case s @ Success(_) => s
      case Failure(err)   => Failure(fn.applyOrElse(err, (_: Throwable) => err)) // out, accursed gremlins of variance!

  /** Transform this to a list which is empty in the failure case
    * @return
    *   a [[List]]
    */
  def toList: List[T] =
    self match
      case Success(t) => List(t)
      case Failure(_) => List.empty

  /** Transform this to a disjunction, applying a transformation to the failure exception.
    *
    * @param f
    *   the exception transformation
    * @tparam E
    *   the resulting left type
    * @return
    *   a disjunction
    */
  def disjoin[E](f: Throwable => E): E \/ T =
    self.fold(left compose f, right)

  /** An alias for [[disjoin]]. */
  def toRightDisjunction[A](f: Throwable => A): A \/ T =
    disjoin(f)

  /** An alias for [[disjoin]]. */
  def \/>[A](f: Throwable => A): A \/ T =
    disjoin(f)

  /** Like [[disjoin]] when you want the throwable on the left untransformed
    * @return
    *   a disjunction
    */
  def disjunction: Throwable \/ T = self.fold(left, right)

  /** Do `fn` if this `Try` is a failure. Like `.foreach` but for failures and returns the try afterwards
    *
    * @param fn
    *   side effect for throwable
    * @return
    *   this `Try`
    */
  def tapFailure(fn: Throwable => Unit): Try[T] = self match
    case Success(_) => self
    case Failure(t) => fn(t); self

  /** Transform this to a disjunction, discarding the exception.
    *
    * @param a
    *   the left value
    * @tparam A
    *   the left type
    * @return
    *   a success as a right, else the supplied left
    */
  def \/>|[A](a: => A): A \/ T =
    self.fold(_ => left(a), right)

  /** Replaces the failure exception, if present, with another, initialising the cause of the new exception with the
    * original.
    *
    * Surprising Side Effect: ^^
    *
    * @param t
    *   the new failure
    * @return
    *   the resulting trf
    */
  def |<@~*(t: => Throwable): Try[T] = self match
    case Success(_) => self
    case Failure(f) => Failure(t.initCause(f))

  /** Map, semipartially, over both sides of the [[Try]].
    *
    * @param onError
    *   a partial function to map exceptions
    * @param onSuccess
    *   a function to map success
    * @tparam U
    *   the result type
    * @return
    *   the resulting [[Try]].
    */
  def bimapf[U](onError: PartialFunction[Throwable, Throwable], onSuccess: T => U): Try[U] =
    mapExceptions(onError).map(onSuccess)

  def orThrow: T = self.get

  def toValidNel[E](e: => E): ValidationNel[E, T] = self.toValidationNel.leftMap(_ => NonEmptyList(e))

  def |(t: => T): T = self getOrElse t
end TryOps

/** Enhancements on the `Try` companion module.
  */
final class TryCompanionOps(private val self: Try.type) extends AnyVal:

  /** Constructs a [[Success]] of the provided value.
    *
    * The return type is widened to [[Try]] to help the type inferencer.
    */
  @inline
  def success[A](a: A): Try[A] = Success(a)

  /** Constructs a [[Failure]] with the provided exception.
    *
    * The return type is widened to [[Try]] to help the type inferencer.
    */
  @inline
  def failure[A](err: Throwable): Try[A] = Failure(err)
end TryCompanionOps

/** Enhancements on anything to transform to a try.
  */
final class TryAnyOps[A](private val self: A) extends AnyVal:

  /** Constructs a [[Success]] of the provided value.
    *
    * The return type is widened to [[Try]] to help the type inferencer.
    */
  @inline
  def success: Try[A] = Success(self)

  /** Constructs a [[Failure]] with the provided exception.
    *
    * The return type is widened to [[Try]] to help the type inferencer.
    */
  @inline
  def failure(implicit ev: A <:< Throwable): Try[Nothing] = Failure(ev(self))
end TryAnyOps

/** [[TryOps]] companion. */
object TryOps extends ToTryOps with ToTryCompanionOps with ToTryAnyOps

/** Implicit conversions from [[Try]]s to their ops. */
trait ToTryOps:
  import language.implicitConversions

  /** Implicitly convert from a [[Try]] to its ops. */
  @inline implicit final def ToTryOps[T](t: Try[T]): TryOps[T] = new TryOps(t)

/** Implicit conversions from the `Try` companion module to its ops. */
trait ToTryCompanionOps:
  import language.implicitConversions

  /** Implicitly convert from the `Try` companion module to its ops. */
  @inline implicit final def ToTryCompanionOps(self: Try.type): TryCompanionOps =
    new TryCompanionOps(self)

/** Implicit conversions from anything to its try ops. */
trait ToTryAnyOps:
  import language.implicitConversions

  /** Implicitly convert from anything to its try ops. */
  @inline implicit final def ToTryAnyOps[A](self: A): TryAnyOps[A] =
    new TryAnyOps(self)
