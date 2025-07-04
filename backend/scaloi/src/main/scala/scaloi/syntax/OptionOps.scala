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

import scalaz.std.option.*
import scalaz.syntax.equal.*
import scalaz.syntax.std.option.*
import scalaz.syntax.std.OptionOps as OptionOpz
import scalaz.syntax.validation.*
import scalaz.{Monoid, Order, Semigroup, ValidationNel, \/, *}

import java.util.Optional
import java.lang as jl
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/** Enhancements on options.
  * @param self
  *   the option value
  * @tparam A
  *   the option type
  */
final class OptionOps[A](private val self: Option[A]) extends AnyVal:
  import AnyOps.*
  import OptionOps.{maxMonoid, minMonoid}
  import scalaz.Validation

  /** Map over a function to a nullable value.
    * @param f
    *   the mapping function
    * @tparam B
    *   the result type
    * @return
    *   the resulting option
    */
  @inline def mapNonNull[B >: Null](f: A => B): Option[B] =
    self.flatMap(a => Option(f(a)))

  /** Convert an option into a successful future, if present, else a supplied failure.
    * @param e
    *   the exception if this option is absent
    * @return
    *   an available future
    */
  @inline def toFuture(e: => Exception): Future[A] =
    self.fold(Future.failed[A](e))(Future.successful)

  /** Return this option, if it does not contain the specified value, else None.
    * @param a
    *   the value to remove
    * @return
    *   this option without the specified value
    */
  @inline def -(a: A)(implicit ev: Equal[A]): Option[A] = self.filter(_ =/= a)

  /** Kestrel combinator on the value of an option.
    * @param f
    *   the side-effecting function
    * @tparam B
    *   the result type
    * @return
    *   this option
    */
  @inline def tap[B](f: A => B): Option[A] = self <| { _ foreach f }

  /** An alias for tap.
    */
  @inline def <|?[B](f: A => B): Option[A] = tap(f)

  /** Kestrel combinator on the empty option.
    * @param f
    *   the side-effecting function
    * @tparam B
    *   the result type
    * @return
    *   this option
    */
  @inline def tapNone[B](f: => B): Option[A] = self <| { o => if o.isEmpty then f }

  /** Map this value and return the result or the zeroal zero of the target type.
    * @param f
    *   the transform
    * @tparam B
    *   the result type
    * @return
    *   the mapped value or zero
    */
  @inline def foldZ[B: Zero](f: A => B): B = self.fold(Zero[B].zero)(f)

  /** Return the contained value or else the evidenced zero of [A]. Contrast with `orZero` which requires monoidal
    * evidence.
    * @param Z
    *   zero evidence of [[A]]
    * @return
    *   the contained value or zero
    */
  @inline def orZ(implicit Z: Zero[A]): A = self getOrElse Z.zero

  /** A successful [[scala.util.Try]] of this option if present, or the given failure if empty.
    * @param failure
    *   the [[scala.util.Try]] failure if this option is empty
    * @return
    *   this option as a [[scala.util.Try]]
    */
  @noinline def toTry(failure: => Throwable): Try[A] =
    self.fold[Try[A]](Failure(failure))(Success(_))

  /** An alias for [[toTry]].
    */
  def elseFailure(failure: => Throwable): Try[A] = toTry(failure)

  /** An alias for [[toTry]].
    */
  @inline def <@~*(failure: => Throwable): Try[A] = toTry(failure)

  /** A successful [[scalaz.Validation]] of this option if present, or the given failure if empty.
    * @param failure
    *   the [[scalaz.Failure]] failure if this option is empty
    * @return
    *   this option as a [[scalaz.Validation]]
    */
  def elseInvalid[B](failure: => B): Validation[B, A] = self match
    case None    => failure.failure[A]
    case Some(a) => a.success[B]

  /** A successful [[scalaz.ValidationNel]] of this option if present, or the given failure if empty.
    * @param failure
    *   the [[scalaz.Failure[NonEmptyList]]] failure if this option is empty
    * @return
    *   this option as a [[scalaz.ValidationNel]]
    */
  def elseInvalidNel[B](failure: => B): ValidationNel[B, A] =
    elseInvalid(failure).toValidationNel

  /** A successful [[scalaz.Validation]] of this option if empty, or the given failure if present.
    * @param fail
    *   the [[scalaz.Failure]] failure if this option is present
    * @return
    *   this option as a [[scalaz.Validation]]
    */
  def thenInvalid[B](fail: A => B): Validation[B, Unit] = self match
    case Some(a) => fail(a).failure[Unit]
    case None    => ().success[B]

  /** A successful [[scalaz.ValidationNel]] of this option if empty, or the given failure if present.
    * @param fail
    *   the [[scalaz.Failure[NonEmptyList]]] failure if this option is present
    * @return
    *   this option as a [[scalaz.ValidationNel]]
    */
  def thenInvalidNel[B](fail: A => B): ValidationNel[B, Unit] = thenInvalid(fail).toValidationNel

  /** The [[Try]] contained in this option, or a [[Failure]] wrapping the given throwable.
    * @param failure
    *   the [[scala.util.Try]] failure if this option is empty
    * @return
    *   this option as a [[scala.util.Try]]
    */
  def flatToTry[B](failure: => Throwable)(implicit ev: A <:< Try[B]): Try[B] =
    self.cata(ev, Failure(failure))

  /** Transforms `target` with the contained function if it exists, otherwise, returns `target`.
    *
    * @param target
    *   the value to be potentially transformed
    * @return
    *   target transformed by the contained function, if it exists, otherwise target.
    */
  def transforming[T](target: T)(implicit ev: A <:< (T => T)): T =
    self.fold(target)(f => ev(f)(target))

  /** An alias for `transforming`.
    */
  @inline def ~?>[T](target: T)(implicit ev: A <:< (T => T)): T =
    this `transforming` target

  /** Flat to left disjunction. Turns this option into a left disjunction if present, or else returns the supplied
    * disjunction.
    * @param f
    *   the disjunction if this option is absent
    * @tparam AA
    *   the left type
    * @tparam B
    *   the right type
    * @return
    *   the resulting disjunction
    */
  @inline def <\/-[AA >: A, B](f: => A \/ B): A \/ B =
    self.toLeftDisjunction(()).flatMap(_ => f)

  /** Turns the contents of this option into a [[Failure]] if present, or succeeds with a given value if absent.
    *
    * @param f
    *   a function to turn this value into a [[Throwable]]
    * @param b
    *   the success value
    * @tparam B
    *   the success type
    * @return
    *   the resulting [[Try]]
    */
  @inline def thenFailure[B](f: A => Throwable, b: => B): Try[B] =
    self.cata(a => Failure(f(a)), Success(b))

  /** Turns the contents of this option into a [[Failure]] if present, or succeeds with a [[Unit]] value if absent.
    *
    * @param f
    *   a function to turn this value into a [[Throwable]]
    * @return
    *   the resulting [[Try]]
    */
  @inline def thenHollowFailure(f: A => Throwable): Try[Unit] =
    self.cata(a => Failure(f(a)), successUnit)

  /** An alias for [[thenHollowFailure]]. */
  @inline def elseHollowVictory(f: A => Throwable): Try[Unit] = thenHollowFailure(f)

  /** A [[scalaz.Validation]] version
    *
    * If this option is present, return [[scalaz.Failure]] with the value. Else (if absent), return [[scalaz.Success]]
    * with the `s` value.
    *
    * @param s
    *   the success value
    * @tparam S
    *   the success type
    * @return
    *   scalaz.Failure(e) if present, scalaz.Success(a) if absent
    */
  @inline def thenInvalid[S](s: S): Validation[A, S] =
    self.cata(_.failure, s.success)

  /** A [[scalaz.ValidationNel]] version
    *
    * If this option is present, return [[scalaz.Failure(NonEmptyList)]] with value. Else (if absent), return
    * [[scalaz.Success(NonEmptyList)]] with the `s` value.
    *
    * @param s
    *   the success value
    * @tparam S
    *   the success type
    * @return
    *   scalaz.Failure(NonEmptyList)(e) if present, scalaz.Success(NonEmptyList)(a) if absent
    */
  @inline def thenInvalidNel[S](s: S): ValidationNel[A, S] =
    self.cata(_.failureNel, s.successNel)

  /** Turns the [[Throwable]] in this option into a [[Failure]] if present, or succeeds with a specified value if
    * absent.
    *
    * @param b
    *   the success value
    * @tparam B
    *   the success type
    * @return
    *   the resulting [[Try]]
    */
  @inline def toFailure[B](b: => B)(implicit ev: A <:< Throwable): Try[B] =
    thenFailure(ev: A => Throwable, b)

  /** Returns this, if present, or else optionally the supplied value if non-zero.
    * @param b
    *   the value
    * @tparam B
    *   the value type, with zero evidence. return this or else that
    */
  @inline def orNZ[B >: A: Zero](b: => B): Option[B] = self orElse OptionOps.OptionNZ(b)

  /** Filter the value to be non-zero.
    * @param Z
    *   zero evidence for A return this if non-zero
    */
  @inline def filterNZ(implicit Z: Zero[A]): Option[A] = self.filterNot(Z.isZero)

  /** Map the contents of this option, filtering out any resulting zero.
    * @param f
    *   the map function
    * @tparam B
    *   the result type
    * @return
    *   the resulting option
    */
  def nzMap[B: Zero](f: A => B): Option[B] = self.map(f).filterNot(Zero[B].isZero)

  /** Runs the provided function as a side-effect if this is `None`, returns this option.
    * @param action
    *   the thing to do if this option is none
    * @return
    *   this option
    */
  def -<|[U](action: => U): Option[A] =
    if self.isEmpty then action
    self

  /** Runs one of the provided functions as a side effect depending on whether it is a Some/None and returns this option
    *
    * @param fSome
    *   the thing to do if this is `Some`
    * @param fNone
    *   the thing to do if this is `None`
    * @return
    *   this options
    */
  def catap[U](fSome: A => U, fNone: => U): Option[A] =
    self match
      case Some(value) => fSome(value)
      case None        => fNone
    self

  /** Similar to foreach, but an additional side effect is provided for the `None`
    *
    * @param fSome
    *   thing to do if this is `Some`
    * @param fNone
    *   thing to do if this is `None`
    */
  def foreachTheirOwn[U](fSome: A => U, fNone: => U): Unit = self.cata(fSome, fNone)

  /** Put `self` on the left, and `right` on the right, of an Eitherneitherboth.
    *
    * @param right
    *   the option to put on the right
    * @return
    *   an Eitherneitherboth with `self` on the left and `right` on the right
    */
  @inline def \|/[B](right: Option[B]): A \|/ B =
    scaloi.\|/(self, right)

  @inline def \&/[B](right: Option[B]): Option[A \&/ B] =
    PartialFunction.condOpt((self, right)) {
      case (Some(l), Some(r)) => scalaz.\&/.Both(l, r)
      case (Some(l), None)    => scalaz.\&/.This(l)
      case (None, Some(r))    => scalaz.\&/.That(r)
    }

  /** Append this optional value with another value in a semigroup.
    * @param b
    *   the other value
    * @tparam B
    *   the other type, with semigroup evidence
    * @return
    *   either the other value or the combined values
    */
  private def append[B >: A: Semigroup](b: B): B = self.fold(b)(Semigroup[B].append(_, b))

  /** Get the maximum of two optional values.
    * @param b
    *   the other optional value
    * @tparam B
    *   the other type
    * @return
    *   the max of the optional values
    */
  @inline def max[B >: A: Order](b: Option[B]): Option[B] = maxMonoid.append(self, b)

  /** Get the maximum of this and a value.
    * @param b
    *   the other value
    * @tparam B
    *   the other type
    * @return
    *   the max of the values
    */
  @inline def max[B >: A: Order](b: B): B = append(b)(using misc.Semigroups.maxSemigroup)

  /** Get the minimum of two optional values.
    * @param b
    *   the other optional value
    * @tparam B
    *   the other type
    * @return
    *   the min of the optional values
    */
  @inline def min[B >: A: Order](b: Option[B]): Option[B] = minMonoid.append(self, b)

  /** Get the minimum of this and a value.
    * @param b
    *   the other value
    * @tparam B
    *   the other type
    * @return
    *   the min of the values
    */
  @inline def min[B >: A: Order](b: B): B = append(b)(using misc.Semigroups.minSemigroup)

  /** Wrap the contained value in a `Gotten`, or create one with the provided thunk and wrap it in a `Created.`
    * @param b
    *   the computation to create a value
    * @tparam B
    *   the type of the created value
    * @return
    *   the contained gotten value, or the supplied created value
    */
  @inline def orCreate[B >: A](b: => B): GetOrCreate[B] = self match
    case Some(gotten) => GetOrCreate.gotten(gotten)
    case None         => GetOrCreate.created(b)

  /** Filter this option by a boolean condition being true.
    * @param b
    *   the condition
    * @return
    *   this, if `b`, else `None`
    */
  @inline def when(b: Boolean): Option[A] = if b then self else None

  /** Filter this option by a boolean condition being false.
    * @param b
    *   the condition
    * @return
    *   this, if not `b`, else `None`
    */
  @inline def unless(b: Boolean): Option[A] = if b then None else self

  /** Accepts the contents of this option if of the specified runtime type.
    * @tparam B
    *   the target type
    * @return
    *   this option if it contains the target type, or else none
    */
  @inline def accept[B: ClassTag]: Option[B] = self.flatMap(implicitly[ClassTag[B]].unapply)

  /** Map this value to a right, if present, else return a supplied left.
    *
    * @param fa
    *   a function to apply to the right value
    * @param c
    *   the value to use for a left
    * @tparam B
    *   the right type
    * @tparam C
    *   the left type
    * @return
    *   the resulting disjunction
    */
  @inline def disjunct[B, C](fa: A => B, c: => C): C \/ B = self.map(fa).toRightDisjunction(c)

  /** Divine whether this option contains truth.
    *
    * @param ev
    *   evidence that the content type is booleate
    * @return
    *   whether this option contains truth
    */
  @inline def isTrue(implicit ev: Booleate[A]): Boolean = self.cata(ev.value, false)

  /** Divine whether this option contains falsity.
    *
    * @param ev
    *   evidence that the content type is booleate
    * @return
    *   whether this option contains falsity
    */
  @inline def isFalse(implicit ev: Booleate[A]): Boolean = self.cata(ev.unvalue, false)

  /** Returns whether this and the supplied option contain the same value. Aka intersects.
    *
    * @param o
    *   the other option
    * @param Equal
    *   equality evidence
    * @return
    *   equality
    */
  @inline def coequals(o: Option[A])(implicit Equal: Equal[A]): Boolean =
    self.exists(a => o.exists(a === _))

  /** An alias for [[coequals]]. */
  @inline def =&=(o: Option[A])(implicit Equal: Equal[A]): Boolean = coequals(o)

  /** An alias for [[orElse]]. */
  @inline def ||[B >: A](o: => Option[B]): Option[B] = self orElse o

  /** Prepend the option value, if any, to a list. */
  @inline def ::?[AA >: A](aas: List[AA]): List[AA] = self match
    case Some(a) => a :: aas
    case None    => aas

  /** Prepend the option values, if any, to a list. */
  @inline def :::?[B](aas: List[B])(implicit Ev: A <:< List[B]): List[B] = self match
    case Some(a) => Ev(a) ::: aas
    case None    => aas

  /** Append this to a list. */
  @inline def ?::[AA >: A](aa: List[AA]): List[AA] = self match
    case Some(a) => aa ::: a :: Nil
    case None    => aa

  /** Append this to an element. */
  @inline def ?:[AA >: A](aa: AA): List[AA] = self match
    case Some(a) => aa :: a :: Nil
    case None    => aa :: Nil

  /** Maps the contained value to a nullable box type and safely unboxes that. */
  @inline def unboxMap[B <: AnyVal, C >: Null <: AnyRef](f: A => C)(implicit C: misc.Boxes[B, C]): Option[B] =
    mapNonNull(f).map(C.unbox)

  @noinline def orThrow(failure: => Throwable): A =
    self.getOrElse(throw failure)
end OptionOps

final class OptionAnyOps[A](private val self: A) extends AnyVal:

  /** Wrap this in a java [[Optional]]. As `.some` is to Scala, this is to Java. */
  def jome: Optional[A] = Optional.of(self)

  /** Wrap a nullable in an [[Option]]. */
  def option: Option[A] = Option(self)

  /** Wrap a nullable zeroable in a non-zero [[Option]]. */
  def optionNZ(implicit Z: Zero[A]): Option[A] = Option(self).filterNot(Z.isZero)
end OptionAnyOps

/** Option operations companion.
  */
object OptionOps extends ToOptionOps

/** Implicit conversion for option operations.
  */
trait ToOptionOps extends Any:

  /** Implicit conversion from option to the option enhancements.
    *
    * @param o
    *   the optional thing
    * @tparam A
    *   its type
    */
  implicit def toOptionOps[A](o: Option[A]): OptionOps[A] = new OptionOps(o)

  /** Implicit conversion from Java optional to the option enhancements.
    *
    * @param o
    *   the optional thing
    * @tparam A
    *   its type
    */
  implicit def toOptionalOps[A >: Null](o: Optional[A]): OptionOps[A] = new OptionOps(Option(o.orElse(null)))

  /** Implicit conversion from Java optional to the option enhancements.
    *
    * @param o
    *   the optional thing
    * @tparam A
    *   its type
    */
  implicit def toOptionalOpz[A >: Null](o: Optional[A]): OptionOpz[A] = new OptionOpz(Option(o.orElse(null)))

  /** Implicit conversion from anything to the option enhancements.
    *
    * @param a
    *   the thing
    * @tparam A
    *   its type
    */
  implicit def toOptionAnyOps[A](a: A): OptionAnyOps[A] = new OptionAnyOps(a)

  /** Returns some if a value is non-null and non-zero, or else none.
    *
    * @param a
    *   the value
    * @tparam A
    *   the value type with zero evidence
    * @return
    *   the option
    */
  def OptionNZ[A: Zero](a: A): Option[A] = Option(a).filterNZ

  /** Returns `Some` if a value is non-null, or else `None`.
    *
    * This differs from [[Option.apply]] in that the type argument of the returned `Option` changes from the boxed
    * argument type [[A]] to the unboxed type [[U]], reflecting the newly-proved nonnullability of the contained value.
    */
  def Boxtion[A >: Null, U <: AnyVal](a: A)(implicit A: misc.Boxes[U, A]): Option[U] =
    Option(a).map(A.unbox)

  /** Monoid evidence for the minimum over an option of an ordered type. */
  def minMonoid[A: Order]: Monoid[Option[A]] = optionMonoid(using misc.Semigroups.minSemigroup)

  /** Monoid evidence for the maximum over an option of an ordered type. */
  def maxMonoid[A: Order]: Monoid[Option[A]] = optionMonoid(using misc.Semigroups.maxSemigroup)

  /** Run the given partial function, or `None` if it does not match.
    */
  def flondOpt[A, B](a: A)(f: PartialFunction[A, Option[B]]): Option[B] =
    f.applyOrElse(a, (_: A) => None)
end ToOptionOps

/** Is something boolean like. */
private[syntax] trait Booleate[A]:
  def value(a: A): Boolean
  final def unvalue(a: A): Boolean = !value(a)

/** Boolean implicits. */
private[syntax] object Booleate:
  implicit def booleate: Booleate[Boolean]    = b => b
  implicit def jooleate: Booleate[jl.Boolean] = j => j.booleanValue
