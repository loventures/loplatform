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

import scalaz.syntax.std.boolean.*
import scalaz.{@@, Functor, \/}

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.Try

/** Enhancements on anything.
  *
  * @param self
  *   the value
  * @tparam A
  *   the type of A
  */
final class AnyOps[A](private val self: A) extends AnyVal:

  /** Kestrel combinator applies a side-effect to a value and then returns the value.
    * @param f
    *   the side-effect
    * @tparam B
    *   the side-effect result type
    * @return
    *   the original value
    */
  @inline final def tap[B](f: A => B): A =
    f(self); self

  /** Kestrel combinator; an alias for tap.
    */
  @inline final def <|[B](f: A => B): A = tap(f)

  /** Thrush combinator applies a value to a function. This shamelessly duplicates scalaz to make imports cleaner.
    * @param f
    *   the function
    * @tparam B
    *   the result type
    * @return
    *   the result
    */
  @inline final def |>[B](f: A => B): B = f(self)

  /** Kestrel combinator supporting a partial function.
    * @param f
    *   a partial function over the parameter
    * @tparam B
    *   the result type of the partial function
    * @return
    *   the original value
    */
  @inline final def pfTap[B](f: PartialFunction[A, B]): A = tap { f orElse constUnit }

  /** Partial kestrel; an alias for pfTap. */
  @inline final def ∂<|[B](f: PartialFunction[A, B]): A = pfTap(f)

  /** Transform a value with a partial function, returning the original value where undefined.
    * @param f
    *   the value to transform
    * @tparam AA
    *   the result type
    * @return
    *   the value, transformed or not
    */
  @inline final def pfTransform[AA >: A](f: PartialFunction[A, AA]): AA = f.lift(self).getOrElse(self)

  /** Partial thrush; an alias for pfTransform. */
  @inline final def ∂|>[AA >: A](f: A =∂> AA): AA = pfTransform(f)

  /** Transform this value if a boolean is true.
    *
    * @param test
    *   the test
    * @param transform
    *   the transformation
    * @tparam B
    *   the transformed type
    * @return
    *   this value, optionally transformed
    */
  @inline final def transformIf[B >: A](test: Boolean)(transform: A => B): B = if test then transform(self) else self

  /** Transform this value only if a predicate holds true.
    *
    * @param pred
    *   the predicate
    * @param transform
    *   the transformation
    * @tparam B
    *   the transformed type
    * @return
    *   this value, optionally transformed
    */
  @inline final def transformWhen[B >: A](pred: A => Boolean)(transform: A => B): B =
    if pred(self) then transform(self) else self

  /** Transform this value only if a predicate holds false.
    *
    * @param pred
    *   the predicate
    * @param transform
    *   the transformation
    * @tparam B
    *   the transformed type
    * @return
    *   this value, optionally transformed
    */
  @inline final def transformUnless[B >: A](pred: A => Boolean)(transform: A => B): B =
    if pred(self) then self else transform(self)

  /** `b` if `self` is not null, or the zeroal zero otherwise.
    *
    * @param b
    *   the computation to return if `self` is non-null
    * @param Z
    *   evidence that `B` has a zero
    * @param ev
    *   evidence that `A` might be `null`
    * @tparam B
    *   the result type
    * @return
    *   `b` if `self` is non-null; `M.zero` otherwise.
    */
  @inline final def ?|[B](b: => B)(implicit Z: Zero[B], ev: Null <:< A): B =
    if self == ev(null) then Z.zero else b

  /** Transform this value only if non-zero.
    *
    * @param transform
    *   the transformation
    * @tparam B
    *   the transformed type
    * @return
    *   this value, optionally transformed
    */
  @inline final def transformNZ[B >: A](transform: A => B)(implicit Z: Zero[A]): B =
    if Z.nonZero(self) then transform(self) else self

  /** Return this value as a right if a predicate is true, or else the supplied left.
    * @param pred
    *   the predicate
    * @param b
    *   the left value
    * @tparam B
    *   the left type
    * @return
    *   the resulting disjunction
    */
  def rightWhen[B](pred: A => Boolean)(b: => B): B \/ A = pred(self) either self or b

  /** Return this value as a right if a predicate is false, or else the supplied left.
    * @param pred
    *   the predicate
    * @param b
    *   the left value
    * @tparam B
    *   the left type
    * @return
    *   the resulting disjunction
    */
  def rightUnless[B](pred: A => Boolean)(b: => B): B \/ A = !pred(self) either self or b

  /** Inject `self` to the left of the [[B]]s in `bs`.
    *
    * For example: 1 -*> List(2,3) === List(1 -> 2, 1 -> 3)
    *
    * @param bs
    *   the functor values
    * @tparam B
    *   the content type
    * @tparam C
    *   the functor type
    * @return
    *   the associated values
    */
  def -*>[B, C[_]: Functor](bs: C[B]): C[(A, B)] = Functor[C].strengthL(self, bs)

  /** Optionally cast this value
    * @tparam B
    *   the target type
    * @return
    *   the cast value or [[None]]
    */
  def asInstanceOf_?[B: ClassTag]: Option[B] =
    scala.reflect.classTag[B].unapply(self)

  /** Try to cast this value.
    * @tparam B
    *   the target type
    * @return
    *   the attempted cast value
    */
  def asInstanceOf_![B: ClassTag]: Try[B] =
    import ClassTagOps.*
    import OptionOps.*
    asInstanceOf_?[B] <@~* new ClassCastException(s"$self.getClass is not a ${classTagClass[B]}")

  /** Tag this value with `Tag`.
    *
    * @tparam Tag
    *   the tag to tag with
    * @return
    *   this value, tagged with `Tag`
    */
  def tag[Tag]: A @@ Tag = scalaz.Tag[A, Tag](self)
end AnyOps

/** Any operations companion.
  */
object AnyOps extends ToAnyOps

/** Implicit conversion for any operation.
  */
trait ToAnyOps:

  /** Implicit conversion from anything to the any enhancements.
    * @param a
    *   the thing
    * @tparam A
    *   its type
    */
  implicit def toAnyOps[A](a: A): AnyOps[A] = new AnyOps(a)

  object sins:

    // probably wrong for many reasons, but so right too
    implicit def any2Option[A](a: A): Option[A] = Option(a)
end ToAnyOps
