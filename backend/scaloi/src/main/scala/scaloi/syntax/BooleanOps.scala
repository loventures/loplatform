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

import java.lang as jl

import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.std.{BooleanOps as BooleanOpsZ, BooleanOps2}
import scalaz.{EphemeralStream, Monoid, Validation, ValidationNel, \/}
import scalaz.syntax.validation.*

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/** Enhancements on booleans.
  * @param self
  *   the boolean value
  */
final class BooleanOps(private val self: Boolean) extends AnyVal:

  /** Returns an option value if true, else none.
    * @param f
    *   a function that produces the optional value
    * @tparam A
    *   the value type
    * @return
    *   the optional value
    */
  def flatOption[A](f: => Option[A]): Option[A] = if self then f else None

  /** Returns an option value if false, else none.
    * @param f
    *   a function that produces the optional value
    * @tparam A
    *   the value type
    * @return
    *   the optional value
    */
  def flatNoption[A](f: => Option[A]): Option[A] = if self then None else f

  /** An alias for [[flatOption]]. */
  def ?-?[A](f: => Option[A]): Option[A] = flatOption(f): @inline

  /** Returns an option value if true and nonempty, otherwise monoidal zero.
    *
    * @param f
    *   a function that produces the optional value
    * @tparam A
    *   the value type
    * @return
    *   the monoidal value
    */
  def ???[A](f: => Option[A])(implicit A: Monoid[A]): A = self.fold(f.orZero, A.zero)

  /** Run a side-effecting function if true.
    * @param f
    *   the side-effecting function
    * @tparam A
    *   the return type
    * @return
    *   the original boolean value
    */
  def <|?[A](f: => A): Boolean =
    if self then f
    self

  /** Run a side-effecting function if false.
    * @param f
    *   the side-effecting function
    * @tparam A
    *   the return type
    * @return
    *   the original boolean value
    */
  def <|![A](f: => A): Boolean =
    if !self then f
    self

  /** Returns the specified value as a left if this is true, else unit right.
    * @param a
    *   the left value
    * @tparam A
    *   the left type
    * @return
    *   the left value or unit
    */
  def thenLeft[A](a: => A): A \/ Unit = (!self).either(()).or(a)

  /** An alias for [[thenLeft]]. */
  @inline def \/>![A](a: A): A \/ Unit = thenLeft(a)

  /** Returns the specified value as a left if this is false, else unit right.
    * @param a
    *   the left value
    * @tparam A
    *   the left type
    * @return
    *   the left value or unit
    */
  def elseLeft[A](a: => A): A \/ Unit = self.either(()).or(a)

  /** An alias for [[elseLeft]]. */
  @inline def \/>[A](a: => A): A \/ Unit = elseLeft(a)

  /** Return an optional value if this is false. The opposite of `.option`.
    * @param a
    *   the value
    * @tparam A
    *   the value type
    * @return
    *   some of the value if this is false
    */
  def noption[A](a: => A): Option[A] = (!self).option(a)

  /** Return an ephemeral stream with the supplied element if this is true, else an empty stream.
    * @param a
    *   the value
    * @tparam A
    *   the value type
    * @return
    *   the stream
    */
  def optionES[A](a: => A): EphemeralStream[A] =
    if self then EphemeralStream(a) else EphemeralStream.emptyEphemeralStream

  /** Return true success if this is true, otherwise fail with the given error.
    *
    * @param err
    *   the error with which to fail
    * @return
    *   `Success(true)` if this is true, or `Failure(err)` otherwise
    */
  def elseFailure(err: => Throwable): Try[Boolean] =
    if self then Success(true) else Failure(err)

  /** An alias for [[elseFailure]].
    */
  @inline def <@~*(err: => Throwable): Try[Boolean] = elseFailure(err)

  /** Return false success if this is false, otherwise fail with the given error.
    *
    * @param err
    *   the error with which to fail
    * @return
    *   `Success(false)` if this is false, or `Failure(err)` otherwise
    */
  def thenFailure(err: => Throwable): Try[Boolean] =
    if self then Failure(err) else Success(false)

  /** An alias for [[thenFailure]].
    */
  @inline def *~@>(err: => Throwable): Try[Boolean] = thenFailure(err)

  /** scalaz.Validation version
    *
    * If self == true, return Validation.success[X, A](that) else return Validation.failure[X, A](err)
    * @param err
    *   the error function
    * @param that
    *   the success value
    * @tparam E
    *   the error return type
    * @tparam A
    *   the success type
    * @return
    *   Validation.success[X, A](that) if true, Validation.failure[X, A](err) if false
    */
  def elseInvalid[E, A](err: => E, that: A): Validation[E, A] =
    if self then that.success[E] else err.failure[A]

  /** Variant of [[elseInvalid]] that fixes the success to [[Unit]]. */
  def elseInvalid[E](err: => E): Validation[E, Unit] = elseInvalid(err, ())

  /** scalaz.Validation version
    *
    * If self == true, return Validation.failure[X, A](err) else return Validation.success[X, A](that)
    * @param err
    *   the error function
    * @param that
    *   the success value
    * @tparam E
    *   the error return type
    * @tparam A
    *   the success type
    * @return
    *   Validation.failure[X, A](err) if true, Validation.success[X, A](that) if false
    */
  def thenInvalid[E, A](err: => E, that: A): Validation[E, A] =
    if self then err.failure[A] else that.success[E]

  /** Variant of [[thenInvalid]] that fixes the success to [[Unit]]. */
  def thenInvalid[E](err: => E): Validation[E, Unit] = thenInvalid(err, ())

  /** scalaz.ValidationNel version
    *
    * If self == true, return ValidationNel.success[X, A](that) else return ValidationNel.failure[X, A](err)
    * @param err
    *   the error function
    * @param that
    *   the success value
    * @tparam E
    *   the error type
    * @tparam A
    *   the success type
    * @return
    *   ValidationNel.success[X, A](that) if true, ValidationNel.failure[X, A](err) if false
    */
  def elseInvalidNel[E, A](err: => E, that: A): ValidationNel[E, A] =
    if self then that.successNel[E] else err.failureNel[A]

  /** Variant of [[elseInvalidNel]] that fixes the success to [[Unit]]. */
  def elseInvalidNel[E](err: => E): ValidationNel[E, Unit] = elseInvalidNel(err, ())

  /** scalaz.ValidationNel version
    *
    * If self == true, return ValidationNel.failure[X, A](err) else return ValidationNel.success[X, A](that)
    * @param err
    *   the error function
    * @param that
    *   the success value
    * @tparam E
    *   the error type
    * @tparam A
    *   the success type
    * @return
    *   ValidationNel.failure[X, A](err) if true, ValidationNel.success[X, A](that) if false
    */
  def thenInvalidNel[E, A](err: => E, that: A): ValidationNel[E, A] =
    if self then err.failureNel[A] else that.successNel[E]

  /** Variant of [[thenInvalidNel]] that fixes the success to [[Unit]]. */
  def thenInvalidNel[E](err: => E): ValidationNel[E, Unit] = thenInvalidNel(err, ())
end BooleanOps

/** Enhancements on boolean conditional eithers.
  *
  * @param self
  *   the conditional either
  * @tparam A
  *   the result type
  */
final class BooleanConditionalEitherOps[A](private val self: BooleanOps2#ConditionalEither[A]) extends AnyVal:

  /** Returns the positive result of the conditional, if true, or else a supplied disjunction value.
    *
    * For example:
    * ```
    * true either "Happy" orElse "Sad".right === Happy.right
    * false either "Happy" orElse "Sad".right === Sad.right
    * false either "Happy" orElse "Sad".left === Sad.left
    * ```
    *
    * @param d
    *   the disjunction value if the conditional is false
    * @tparam B
    *   the left type
    * @return
    *   the resulting disjunction
    */
  def orElse[B](d: => B \/ A): B \/ A = self.or(()).orElse(d)

  import DisjunctionOps.*

  /** Returns the positive result of the conditional, if true, as a success, or else the supplied result as a failure.
    * @param e
    *   the failure return the [[Try]].
    */
  def orFailure(e: => Throwable): Try[A] = self.or(e).toTry

  def orInvalidNel[B](e: => B): ValidationNel[B, A] = self.or(e).fold(_.failureNel, _.successNel)
end BooleanConditionalEitherOps

/** Boolean operations companion.
  */
object BooleanOps extends ToBooleanOps

/** Implicit conversion for boolean operations.
  */
trait ToBooleanOps:

  /** Implicit conversion from a boolean to enhancements.
    * @param value
    *   the boolean
    */
  implicit def toBooleanOps(value: Boolean): BooleanOps = new BooleanOps(value)

  /** Implicit conversion from a boxed boolean to enhancements.
    * @param value
    *   the boolean
    */
  implicit def toBooleanOps(value: jl.Boolean): BooleanOps = new BooleanOps(value.booleanValue)

  /** Implicit conversion from a boxed boolean to scalaz enhancements.
    * @param value
    *   the boolean
    */
  implicit def toBooleanOpz(value: jl.Boolean): BooleanOpsZ = new BooleanOpsZ(value.booleanValue)

  /** Implicit conversion from a boxed boolean to other scalaz enhancements.
    * @param value
    *   the boolean
    */
  implicit def toBooleanOps2(value: jl.Boolean): BooleanOps2 = new BooleanOps2(value.booleanValue)

  /** Implicit conversion from boolean conditional either to the enhancements.
    * @param e
    *   the conditional either
    * @tparam A
    *   its type
    */
  implicit def toBooleanConditionalEither[A](e: BooleanOps2#ConditionalEither[A]): BooleanConditionalEitherOps[A] =
    new BooleanConditionalEitherOps(e)
end ToBooleanOps
