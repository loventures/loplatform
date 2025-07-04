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

import scaloi.syntax.mutableMap.*

import scala.collection.mutable
import scala.concurrent.duration.*

/** Accumulated SQL statistics.
  */
class SqlStatistics:

  /** The per-SQL-statement statistics. Thread scoped so unsynchronized. */
  private val statements = mutable.Map.empty[String, Statistic]

  /** Get the accumulated SQL statistics.
    *
    * @return
    *   the SQL statements and their statistics
    */
  def statistics: Seq[(String, Statistic)] = statements.toSeq

  /** Get whether the statistics are non-empty.
    *
    * @return
    *   whether the statistics are non-empty
    */
  def nonEmpty: Boolean = statements.nonEmpty

  /** Get the count of distinct SQL statements.
    *
    * @return
    *   the count of distinct SQL statements
    */
  def distinctCount: Int = statements.size

  /** Get the total count of SQL statements executed.
    *
    * @return
    *   the total count of SQL statements executed
    */
  def count: Int = statements.values.map(_.count).sum

  /** Get the cumulative time spent executing SQL statements.
    *
    * @return
    *   the cumulative time
    */
  def duration: FiniteDuration = statements.values.map(_.duration).sum.nanos

  /** Generate a multiline text report of the statistics. */
  def report: String = statistics.sortBy(_._2.count).map(reportStr).mkString("\n")

  /** Convert statement information to a string. */
  private def reportStr(tuple: (String, Statistic)): String =
    val (sql, Statistic(count, ns)) = tuple
    s"#$count, ${ns / ONE_MEELLION} ms, $sql"

  /** Record that a SQL statement was executed.
    *
    * @param sql
    *   the statement
    * @param ms
    *   the runtime
    */
  private[mon] def record(sql: String, ms: Long): Unit = statements.append(sql, Statistic(ms))

  /** One million, for converting from nanos to millis. */
  private final val ONE_MEELLION = 1000000L
end SqlStatistics
