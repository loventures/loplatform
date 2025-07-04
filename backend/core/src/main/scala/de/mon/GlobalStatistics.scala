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

package de.mon

import scaloi.syntax.CollectionOps.*

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import scala.jdk.CollectionConverters.*

/** Records global DE monitoring statistics.
  */
object GlobalStatistics:

  /** A map from entity statistic keys to the monitored statistics. */
  private final val stats = new ConcurrentHashMap[EntityStatistic, ConcurrentStatistic]

  /** Record a statistic. */
  @inline private def add(stat: EntityStatistic, duration: Long): Unit =
    stats.computeIfAbsent(stat, _ => new ConcurrentStatistic).add(duration)

  /** Record that a statement was executed.
    *
    * @param sql
    *   the SQL statement
    * @param duration
    *   the execution duration in nanoseconds
    */
  private[mon] def recordSql(sql: String, duration: Long): Unit =
    add(SqlRedux.tableStatement(sql.toLowerCase), duration)

  /** Record a global statistic.
    *
    * @param tpe
    *   the statistic type
    * @param name
    *   the entity name
    * @param duration
    *   the duration
    */
  private[mon] def record(tpe: StatisticType, name: String, duration: Long): Unit =
    add(tpe -> name, duration)

  /** Get the global statistics. */
  def statistics: Map[EntityStatistic, GlobalStatistic] =
    stats.entrySet.iterator.asScala.foldToMap(entry => entry.getKey -> entry.getValue.toStatistic)

  /** Clear the global statistics. */
  def reset(): Unit = stats.clear()

  /** Update the counts for rate tracking. */
  def updateRate(): Unit = stats.forEachValue(Long.MaxValue, stat => stat.updateRate())

  /** Concurrent statistic. */
  private class ConcurrentStatistic:

    /** Total accumulated statistic count. */
    private val count = new LongAdder()

    /** Total accumulated duration. */
    private val duration = new LongAdder()

    /** Rate recording. */
    private var rate = (0, 0)

    /** Add a duration. Doesn't guarantee atomicity of count and duration accumulators. */
    def add(dur: Long): Unit =
      count.increment()
      duration.add(dur)

    /** Update counts for rate purposes. */
    def updateRate(): Unit = rate = (rate._2, count.intValue)

    /** Convert to immutable statistic. */
    def toStatistic: GlobalStatistic = GlobalStatistic(count.intValue, duration.longValue, rate._2 - rate._1)
  end ConcurrentStatistic
end GlobalStatistics

/** Snapshot of a global statistic. */
final case class GlobalStatistic(count: Int, duration: Long, rate: Int)
