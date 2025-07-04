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

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}
import scalaz.{-\/, \/, \/-}
import scaloi.syntax.AnyOps.*
import scalaz.syntax.bind.*

/** Enhancements on disjunctions.
  *
  * @param self
  *   the disjunction
  * @tparam A
  *   the left type
  * @tparam B
  *   the right type
  */
final class DisjunctionOps[A, B](private val self: A \/ B) extends AnyVal:

  /** Left tap: apply a kestrel combinator to the left value of a disjunction. Runs a side-effect on the left value and
    * returns this disjunction unchanged.
    *
    * @param f
    *   the function to apply to the left value
    * @tparam T
    *   the result type of the function
    * @return
    *   the left value
    */
  def leftTap[T](f: A => T): A \/ B = self leftMap {
    _ <| f
  }

  /** An alias for leftTap.
    */
  @inline final def -<|[T](f: A => T): A \/ B = leftTap(f)

  /** Right tap: apply a kestrel combinator to the right value of a disjunction. Runs a side-effect on the right value
    * and returns this disjunction unchanged.
    *
    * @param f
    *   the function to apply to the right value
    * @tparam T
    *   the result type of the function
    * @return
    *   the right value
    */
  def rightTap[T](f: B => T): A \/ B = self map {
    _ <| f
  }

  /** An alias for rightTap.
    */
  @inline final def <|-[T](f: B => T): A \/ B = rightTap(f)

  /** An alias for leftMap.
    */
  @inline final def -\/>[C](f: A => C): C \/ B = self leftMap f

  /** Return this disjunction if a left, otherwise the supplied disjunction.
    * @param d
    *   the supplied disjunction
    * @return
    *   this, if left, or else that
    */
  def andThen[C](d: => A \/ C): A \/ C = self.flatMap(_ => d)

  /** An alias for andThen for use in arrow programming.
    */
  @inline final def |-->[C](d: => A \/ C): A \/ C = andThen(d)

  /** An alias for flatMap for use in arrow programming.
    */
  @inline final def -->[C](d: B => A \/ C): A \/ C = self.flatMap(d)

  /** Return this disjunction if a right, otherwise the disjunction produced by applying a function to the left value.
    * Like [MonadError#handleError] but allows the types to change.
    * @param f
    *   the function
    * @tparam C
    *   the new left type
    * @tparam BB
    *   the new right type
    * @return
    *   this, if right, else f of the left
    */
  def leftFlatMap[C, BB >: B](f: A => C \/ BB): C \/ BB = self match
    case -\/(a) => f(a)
    case \/-(b) => \/-(b)

  /** Get the right value or throw the throwable from the left value.
    *
    * @param ev
    *   evidence for the throwable of the left type
    * @return
    *   the right value
    */
  def orThrow(implicit ev: A <:< Throwable): B = self.valueOr(a => throw ev(a))

  /** Convert this to a `Try` if the left is a throwable.
    *
    * @param ev
    *   evidence for the throwable of the left type
    * @return
    *   the resulting `Try`
    */
  @noinline
  def toTry(implicit ev: A <:< Throwable): Try[B] = self.fold(e => Failure(ev(e)), Success.apply)

  /** Convert this to a `Try`, explicitly converting the left to an exception.
    *
    * @param ex
    *   function to turn the left into an exception
    * @return
    *   the resulting `Try`
    */
  @noinline
  def toTry(ex: A => Throwable): Try[B] = self.fold(e => Failure(ex(e)), Success.apply)

  /** Convert this to a `Future` if the left is a throwable.
    *
    * @param ev
    *   evidence for the throwable of the left type
    * @return
    *   the resulting `Try`
    */
  def toFuture(implicit ev: A <:< Throwable): Future[B] = self.fold(e => Future.failed(ev(e)), Future.successful)

  /** Convert this to a `Future`, explicitly converting the left to an exception.
    *
    * @param ex
    *   function to turn the left into an exception
    * @return
    *   the resulting `Try`
    */
  def toFuture(ex: A => Throwable): Future[B] = self.fold(e => Future.failed(ex(e)), Future.successful)

  /** Evaluate `f` on the right, catching errors, and join everything together.
    *
    * @param f
    *   the function to apply to the right value
    * @return
    *   the result of `f` flat-mapped into self, or an error on the left.
    */
  def tryFlatMap[T](f: B => A \/ T)(implicit liskaa: A <:< Throwable, liska: A <:< Throwable): Throwable \/ T =
    self.leftMap(liska).flatMap(b => \/.attempt(f(b) leftMap liskaa)(identity).join)

  /** Runs a side-effecting function on whichever side of the disjunction is present.
    * @param fa
    *   the function to run on the left
    * @param fb
    *   the function to run on the right
    * @tparam C
    *   the left result type
    * @tparam D
    *   the right result type
    */
  def biforeach[C, D](fa: A => C, fb: B => D): Unit = self.fold[Any](fa, fb)

  /** Pair the left or right with the result of function application.
    * @param fa
    *   the function to run on the left
    * @param fb
    *   the function to run on the right
    * @tparam C
    *   the left result type
    * @tparam D
    *   the right result type
    * @return
    *   the paired left or right
    */
  def bifproduct[C, D](fa: A => C, fb: B => D): (A, C) \/ (B, D) =
    self.bimap(a => a -> fa(a), b => b -> fb(b))
end DisjunctionOps

/** Disjunction operations companion.
  */
object DisjunctionOps extends ToDisjunctionOps with DisjunctionFns

/** Disjunction helper functions.
  */
trait DisjunctionFns:

  /** Try to evaluate a function, returning either the result or any non-fatal error that was thrown. Pronounced
    * try-ther.
    * @param f
    *   a function producing a value
    * @tparam T
    *   the function result type
    * @return
    *   either a throwable or the function result
    */
  def treither[T](f: => T): Throwable \/ T = \/.attempt(f)(identity)

  /** An alias for treither.
    */
  @inline final def \@~*/[T](f: => T): Throwable \/ T = treither(f)
end DisjunctionFns

/** Implicit conversion for disjunction operations.
  */
trait ToDisjunctionOps:

  /** Implicit conversion from disjunction to the disjunction enhancements.
    * @param d
    *   the disjunction value
    * @tparam A
    *   the left type
    * @tparam B
    *   the right type
    */
  implicit def toDisjunctionOps[A, B](d: A \/ B): DisjunctionOps[A, B] = new DisjunctionOps(d)
end ToDisjunctionOps
