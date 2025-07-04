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

import scalaz.{Applicative, Bitraverse, \/}
import scalaz.syntax.either.*

/** Either, neither or both type. Represents an [[A]], a [[B]], both an [[A]] and a [[B]], or neither. Contrast with
  * [[scalaz.\&/]] which does not admit the possibility of neither and [[scalaz.\/]] which further denies the
  * possibility of both. This is isomorphic with [[Option[scalaz.\&/]]] but less opaque.
  *
  * @tparam A
  *   the left possible type
  * @tparam B
  *   the right possible type
  */
sealed abstract class \|/[A, B] extends Product with Serializable:
  import \|/.*

  /** Get this, if present. */
  def thisOption: Option[A] = PartialFunction.condOpt(this) {
    case This(a)    => a
    case Both(a, _) => a
  }

  /** Get that, if present. */
  def thatOption: Option[B] = PartialFunction.condOpt(this) {
    case That(b)    => b
    case Both(_, b) => b
  }

  /** Get either, if present, but not both. */
  def eitherOption: Option[A \/ B] = PartialFunction.condOpt(this) {
    case This(a) => a.left
    case That(b) => b.right
  }

  /** Map this and that. */
  def bimap[C, D](f: A => C, g: B => D): C \|/ D =
    \|/(thisOption map f, thatOption map g)

  /** Traverse this and that. */
  def bitraverse[F[_]: Applicative, C, D](f: A => F[C], g: B => F[D]): F[C \|/ D] = this match
    case Neither()  => Applicative[F].point(Neither())
    case This(a)    => Applicative[F].apply(f(a))(This.apply)
    case That(b)    => Applicative[F].apply(g(b))(That.apply)
    case Both(a, b) => Applicative[F].apply2(f(a), g(b))(Both.apply)

  /** Is this only one of the two values? */
  def isOnlyOne: Boolean = this match
    case This(_) | That(_) => true
    case _                 => false
end \|/

/** Either, neither or both companion. */
object `\\|/`:

  /** Construct an either, neither or both from a pair of options.
    * @param ao
    *   this value, if present
    * @param bo
    *   that value, if present
    * @tparam A
    *   this type
    * @tparam B
    *   that type
    * @return
    *   the either, neither or both
    */
  def apply[A, B](ao: Option[A], bo: Option[B]): A \|/ B = (ao, bo) match
    case (None, None)       => Neither()
    case (Some(a), None)    => This(a)
    case (None, Some(b))    => That(b)
    case (Some(a), Some(b)) => Both(a, b)

  /** When neither option is present. */
  sealed abstract case class Neither[A, B] private () extends (A \|/ B):
    def coerce[C, D]: C \|/ D = this.asInstanceOf[C \|/ D]

  object Neither:
    private val value          = new Neither[Nothing, Nothing] {}
    def apply[A, B](): A \|/ B = value.coerce[A, B]

  /** When only this value is present. */
  final case class This[A, B](a: A) extends (A \|/ B):
    def coerceThat[C]: A \|/ C = this.asInstanceOf[A \|/ C]

  /** When only that value is present. */
  final case class That[A, B](b: B) extends (A \|/ B):
    def coerceThis[C]: C \|/ B = this.asInstanceOf[C \|/ B]

  /** When both values are present. */
  final case class Both[A, B](a: A, b: B) extends (A \|/ B)

  /** Bitraverse evidence for ENBs.
    */
  implicit val BitraverseENB: Bitraverse[\|/] = new Bitraverse[\|/]:
    override def bimap[A, B, C, D](ab: A \|/ B)(f: A => C, g: B => D): C \|/ D = ab.bimap(f, g)

    override def bitraverseImpl[F[_]: Applicative, A, B, C, D](ab: A \|/ B)(f: A => F[C], g: B => F[D]): F[C \|/ D] =
      ab.bitraverse(f, g)
end \|/
