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

import java.sql.Timestamp
import java.util.Date
import scalaz.*

import java.time.LocalDateTime
import scala.concurrent.duration.*
import scala.language.implicitConversions

/** Enhancements on dates.
  * @param self
  *   the date
  */
final class DateOps(private val self: Date) extends AnyVal:

  /** Add a duration to a date.
    * @param s
    *   the duration
    * @return
    *   the new date
    */
  def +(s: Duration): Date = new Date(self.getTime + s.toMillis)

  /** Subtract a duration from a date.
    * @param s
    *   the duration
    * @return
    *   the new date
    */
  def -(s: Duration): Date = new Date(self.getTime - s.toMillis)

  /** Subtract a date from a date.
    * @param other
    *   the other date
    * @return
    *   the difference between the dates
    */
  def -(other: Date): FiniteDuration = (self.getTime - other.getTime).millis

  /** Return how far this date is from now.
    * @param ts
    *   a time source
    * @return
    *   the duration from now to this date
    */
  def fromNow(implicit ts: misc.TimeSource): FiniteDuration = this.-(ts.date)

  /** Convert to a [[Timestamp]].
    * @return
    *   this date as a [[Timestamp]]
    */
  def toTimestamp: Timestamp = new Timestamp(self.getTime)

  def asLocalDateTime: LocalDateTime = toTimestamp.toLocalDateTime
end DateOps

/** Date operations companion.
  */
object DateOps extends ToDateOps with DateInstances

/** Date instances. */
trait DateInstances:

  /** Order evidence for dates.
    */
  implicit val DateOrder: Order[Date] =
    (d1, d2) => Ordering.fromInt(d1 `compareTo` d2)

/** Implicit conversion for date operations.
  */
trait ToDateOps:

  /** Implicit conversion from a date to enhancements.
    * @param date
    *   the date
    */
  implicit def toDateOps(date: Date): DateOps = new DateOps(date)
