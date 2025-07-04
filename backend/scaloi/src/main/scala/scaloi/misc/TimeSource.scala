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

import scaloi.syntax.localDateTime.*

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime}
import java.util.Date
import scala.language.implicitConversions

/** Abstraction over the current time. */
trait TimeSource:

  /** Get the current time in milliseconds. */
  def time: Long

  /** Get the current time as a date. */
  def date: Date = new Date(time) // do this with TimeFormat magnets?

  /** Get the current time as a timestamp. */
  def timestamp: Timestamp = new Timestamp(time)

  /** Get the current time as an instant. */
  def instant: Instant = date.toInstant

  def localDateTime: LocalDateTime = timestamp.toLocalDateTime

  def <[T: TimeyWimey](t: T): Boolean  = time < TimeyWimey[T].time(t)
  def <=[T: TimeyWimey](t: T): Boolean = time <= TimeyWimey[T].time(t)
  def >=[T: TimeyWimey](t: T): Boolean = time >= TimeyWimey[T].time(t)
  def >[T: TimeyWimey](t: T): Boolean  = time > TimeyWimey[T].time(t)
end TimeSource

/** Time source companion. */
object TimeSource:

  /** Evidence of `System` as a source of truth for the time. Captures the immutable time at the moment of summonation.
    */
  implicit def ts: TimeSource = new TimeSource():
    override val time: Long = System.currentTimeMillis

  /** A real-time time source that always returns the current wall-clock time.
    */
  def realtime: TimeSource = new TimeSource():
    override def time: Long = System.currentTimeMillis

  def fromInstant(instant0: Instant): TimeSource = new TimeSource():
    override val time: Long = instant0.toEpochMilli

  def fromDate(date0: Date): TimeSource = new TimeSource():
    override val time: Long = date0.getTime

  def fromLocalDateTime(ldt: LocalDateTime): TimeSource = fromDate(ldt.asDate)

  object Conversions:

    implicit def instant2TimeSource(instant: Instant): TimeSource         = fromInstant(instant)
    implicit def localDateTime2TimeSource(ldt: LocalDateTime): TimeSource = fromLocalDateTime(ldt)
end TimeSource

trait TimeyWimey[T]:
  def time(t: T): Long

object TimeyWimey:
  def apply[T](implicit T: TimeyWimey[T]): TimeyWimey[T]  = T
  implicit val DateTimeyWimey: TimeyWimey[Date]           = _.getTime
  implicit val TimestampTimeyWimey: TimeyWimey[Timestamp] = _.getTime
  implicit val InstantTimeyWimey: TimeyWimey[Instant]     = _.toEpochMilli
