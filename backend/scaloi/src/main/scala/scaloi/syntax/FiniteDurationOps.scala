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

import java.util.concurrent.TimeUnit
import java.time as jt

import scala.concurrent.duration.*
import scala.language.implicitConversions

import scalaz.syntax.`enum`.*

import misc.JEnumEnum.*

/** Finite duration pimping operations.
  *
  * @param self
  *   the duration to pimp
  */
final class FiniteDurationOps(private val self: FiniteDuration) extends AnyVal:

  /** Convert this to the specified time unit, discarding any fractional part.
    * @param tu
    *   the target time unit
    * @return
    *   the new finite duration
    */
  def toFiniteDuration(tu: TimeUnit): FiniteDuration =
    FiniteDuration(self.toUnit(tu).toLong, tu)

  /** Convert this to a java.time.Duration.
    * @return
    *   a java.time.Duration expressing the same value
    */
  def asJava: jt.Duration =
    val seconds = self.toSeconds
    val nanos   =
      if seconds > 0 then (self - seconds.seconds).toNanos
      else if seconds < 0 then (self + seconds.abs.seconds).toNanos
      else self.toNanos
    jt.Duration.ofSeconds(seconds, nanos)

  /** Find the largest time unit contained by this duration. */
  protected def largestUnit: Option[TimeUnit] =
    TimeUnit.values.reverse.find(u => self.toUnit(u) >= 1.0)

  /** Return a human string representation of this duration. It is scaled to the largest non-zero time unit and then the
    * value is printed in terms of that time unit and the next smaller one. For example "1 minute, 3 seconds".
    */
  def toHumanString: String =
    largestUnit.fold("no time at all") { u =>
      val scaled = toFiniteDuration(u)
      u.predx.fold(scaled.toString) { v =>
        val modulus   = FiniteDuration(1, u).toUnit(v).toInt
        val remainder = self.toUnit(v).toLong % modulus
        if remainder > 0 then scaled.toString + ", " + FiniteDuration(remainder, v)
        else scaled.toString
      }
    }
end FiniteDurationOps

/** Finite duration operations companion.
  */
object FiniteDurationOps extends ToFiniteDurationOps

/** Implicit conversion for Finite duration operations.
  */
trait ToFiniteDurationOps:

  /** Implicit conversion from finite duration to the duration enhancements.
    * @param d
    *   the finite duration
    */
  implicit def toFiniteDurationOps(d: FiniteDuration): FiniteDurationOps = new FiniteDurationOps(d)
