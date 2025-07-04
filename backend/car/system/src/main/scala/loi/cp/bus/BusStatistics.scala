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

package loi.cp.bus

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scalaz.std.anyVal.*
import scalaz.syntax.semigroup.*
import scalaz.{@@, Monoid, Semigroup, Tags}
import scaloi.misc.TaggedMonoid

import java.lang as jl
import java.time.*
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.{ChronoField, TemporalAmount}
import java.util.Date

// Message statistics for a particular interval
final case class IntervalStatistics(
  // Number of messages delivered.
  delivered: Int,
  // Number of message failures. A given message may fail many times.
  failed: Int,
  // Number of messages dropped.
  dropped: Int,
  // Total time spent delivering messages.
  millis: Long,
  // Message queue length.
  @JsonDeserialize(as = classOf[jl.Long])
  queued: Long @@ Tags.MaxVal,
  // Number of bus executions (meaningful for buses that process messages in bulk)
  executions: Int,
)

object IntervalStatistics:
  // Monoid evidence for the maximum value of longs, with 0 default
  private implicit val maxM: Monoid[Long @@ Tags.MaxVal] =
    TaggedMonoid[Tags.MaxVal](0L)(using Semigroup.maxTaggedSemigroup)

  // Monoid evidence for interval statistics
  implicit val monoid: Monoid[IntervalStatistics] = Monoid.instance(
    (a, b) =>
      IntervalStatistics(
        a.delivered + b.delivered,
        a.failed + b.failed,
        a.dropped + b.dropped,
        a.millis + b.millis,
        a.queued |+| b.queued,
        a.executions + b.executions,
      ),
    IntervalStatistics(0, 0, 0, 0, Tags.MaxVal(0), 0)
  )

  // Message delivered.
  val Delivered: IntervalStatistics = monoid.zero.copy(delivered = 1)

  // Message failed.
  val Failed: IntervalStatistics = monoid.zero.copy(failed = 1)

  // Message dropped.
  val Dropped: IntervalStatistics = monoid.zero.copy(dropped = 1)

  // Interval of time elapsed.
  def elapsed(millis: Long): IntervalStatistics =
    monoid.zero.copy(millis = millis)

  // Total events queued.
  def queued(queued: Long): IntervalStatistics =
    monoid.zero.copy(queued = Tags.MaxVal(queued))
end IntervalStatistics

// A time frame over which statistics are accumulated.
class TimeFrame(
  // Date/time format pattern.
  pattern: String,
  // Period for which statistics over this time frame should be retained.
  val retention: TemporalAmount
):
  // Date/time formatter.
  // the .parseDefaulting is because TimeFrame.Days pattern has no time part at all.
  // A string in such format cannot be parsed into a LocalDateTime, but it can if you tell
  // the formatter to default hours to 0, given a time part.
  final val format = new DateTimeFormatterBuilder()
    .appendPattern(pattern)
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .toFormatter
    .withZone(ZoneId.of("UTC"))
end TimeFrame

object TimeFrame:
  final val Minutes = new TimeFrame("yyyy-MM-dd'T'HH:mm", Duration.ofHours(1))
  final val Hours   = new TimeFrame("yyyy-MM-dd'T'HH", Period.ofDays(1))
  final val Days    = new TimeFrame("yyyy-MM-dd", Period.ofYears(1))

final case class BusStatistics(
  byMinute: Map[String, IntervalStatistics],
  byHour: Map[String, IntervalStatistics],
  byDay: Map[String, IntervalStatistics],
  lastEmail: Option[Date]
):
  // Return revised statistics with interval stats added.
  def +(stats: IntervalStatistics, now: ZonedDateTime = ZonedDateTime.now): BusStatistics =
    copy(
      byMinute = update(byMinute, TimeFrame.Minutes, now, stats),
      byHour = update(byHour, TimeFrame.Hours, now, stats),
      byDay = update(byDay, TimeFrame.Days, now, stats)
    )

  // Return revised statistics with optional email date added.
  def +(email: Option[Date]): BusStatistics = copy(lastEmail = email orElse lastEmail)

  // Add statistics to interval map, removing expired entries.
  private def update(
    map: Map[String, IntervalStatistics],
    frame: TimeFrame,
    now: ZonedDateTime,
    stats: IntervalStatistics
  ): Map[String, IntervalStatistics] =
    val key     = frame.format.format(now)
    val expires = now.minus(frame.retention)
    map.view.filterKeys(retain(expires, frame.format)).toMap +
      (key -> map.get(key).fold(stats)(IntervalStatistics.monoid.append(_, stats)))
  end update

  // Retain non-expired interval map keys.
  private def retain(expires: ZonedDateTime, format: DateTimeFormatter): String => Boolean =
    format.parse[LocalDateTime](_, LocalDateTime.from).isAfter(expires.toLocalDateTime)
end BusStatistics

object BusStatistics:
  final val Zero = BusStatistics(Map.empty, Map.empty, Map.empty, None)
