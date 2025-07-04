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

/** Per-thread SQL statistics.
  */
object ThreadStatistics:

  /** Start recording monitoring statistics for the current thread. */
  def start(): Unit = stats.set(Some(new SqlStatistics))

  /** Stop recording monitoring statistics for the current thread. */
  def stop(): Unit = stats.remove()

  /** Get monitoring statistics for the current thread.
    *
    * @return
    *   monitoring statistics, if available
    */
  def statistics: Option[SqlStatistics] = stats.get

  /** Thread-local statistics. */
  private final val stats = ThreadLocal.withInitial[Option[SqlStatistics]](() => None)
end ThreadStatistics
