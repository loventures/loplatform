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
package misc

import java.time.Instant

import enumeratum.EnumEntry
import scaloi.syntax.StringOps.*

import scala.util.{Success, Try}

/** A stringomorphism represents a transformation from a string to a type, or an error if the transformation falise.
  * @tparam T
  *   the target type
  */
trait Stringomorphism[T] extends (String => Try[T])

object Stringomorphism:
  def apply[T](implicit st: Stringomorphism[T]): Stringomorphism[T] = st

  /** A homomomorphism for strings. */
  implicit val Stringhomorphism: Stringomorphism[String] = s => Success(s)

  /** A stringomorphism to an [[Instant]]. */
  implicit val InstantStringomorphism: Stringomorphism[Instant] = s => Try(Instant.parse(s))

  /** A stringomorphism to a [[Boolean]]. */
  implicit val BooleanStringomorphism: Stringomorphism[Boolean] = s => Try(s.toBoolean)

  /** A stringomorphism to a [[Long]]. */
  implicit val LongStringomorphism: Stringomorphism[Long] = _.toLong_!

  /** A stringomorphism to an enumeratum [[EnumEntry]]. */
  implicit def enumeratumStringomorphism[E <: EnumEntry: Enumerative]: Stringomorphism[E] =
    s => Try(Enumerative[E].`enum`.withName(s))
end Stringomorphism
