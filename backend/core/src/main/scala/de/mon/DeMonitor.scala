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

/** Methods to record that monitorable things occurred.
  */
object DeMonitor:

  /** Record that a SQL statement was executed.
    *
    * @param sql
    *   the SQL
    * @param duration
    *   the runtime in nanoseconds
    */
  def sqlExecuted(sql: String, duration: Long): Unit =
    GlobalStatistics.recordSql(sql, duration)
    ThreadStatistics.statistics foreach {
      // TODO: should i simplify the SQL first? i could collapse IN (?, ?, ?, ?) to IN (???) and many
      // generated statements would coalesce better.
      _.record(sql, duration)
    }

  /** Record that a table was evicted.
    *
    * @param name
    *   the table
    */
  def tableEvicted(name: String): Unit =
    if "Item".equalsIgnoreCase(name) then
      logger warn "Item table was evicted. This is bad and likely to cause performance problems if repeated."
    recordGlobalStatistic(StatisticType.Evict, name, 0)

  /** Record a global statistic.
    *
    * @param stmt
    *   the statement type
    * @param name
    *   the table name
    * @param duration
    *   the duration
    */
  def recordGlobalStatistic(stmt: StatisticType, name: String, duration: Long): Unit =
    GlobalStatistics.record(stmt, name, duration)

  /** The logger. */
  private final val logger = org.log4s.getLogger
end DeMonitor
