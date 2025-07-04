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

import scalaz.syntax.std.option.*
import scalaz.{MonadPlus, Monoid}

/** Operations on [[PartialFunction]]s.
  */
final class PartialFunctionOps[A, R](private val self: A =∂> R) extends AnyVal:

  /** Apply this partial function to `a`, or return `default` if not defined.
    *
    * This method exists because [[PartialFunction.applyOrElse]] has a truly evil signature replete with variance,
    * bounded type parameters, and other inference-confounding misfeatures.
    *
    * @param a
    *   the value to apply this partial function to
    * @param default
    *   the default value
    * @return
    *   the result, or the default
    */
  def applyOrDefault(a: A, default: => R): R =
    self.applyOrElse[A, R](a, _ => default)

  /** Apply this partial function to `a`, or return the monoidal zero.
    *
    * @param a
    *   the value
    * @param R
    *   the monoid
    * @return
    *   the result, or monoidal zero.
    */
  def applyOrZero(a: A)(implicit R: Monoid[R]): R =
    applyOrDefault(a, R.zero)

  /** Apply this partial function inside of a [[MonadPlus]].
    *
    * @param a
    *   the value
    * @param F
    *   the monadic plusity
    * @tparam F
    *   the monad plus
    * @return
    *   the result, or monadic plush emptiness.
    */
  def mapply[F[_]](a: F[A])(implicit F: MonadPlus[F]): F[R] =
    F.bind(a)(self.lift.apply(_).cata(F.point(_), F.empty))
end PartialFunctionOps

trait ToPartialFunctionOps:
  import language.implicitConversions

  @inline
  implicit final def ToPartialFunctionOps[A, R](pf: A =∂> R): PartialFunctionOps[A, R] =
    new PartialFunctionOps[A, R](pf)
