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

import scalaz.Monoid

/** Encapsulates a count and a nanosecond runtime.
  *
  * @param count
  *   the count
  * @param duration
  *   the cumulative duration in nanoseconds
  */
case class Statistic(count: Int, duration: Long):

  /** Add a statistic to this statistic.
    * @param stat
    *   the statistic to add
    * @return
    *   the summed statistics
    */
  def +(stat: Statistic): Statistic = Statistic(stat.count + count, stat.duration + duration)

/** Stat companion. */
object Statistic:

  /** Create a single statistic.
    * @param duration
    *   the duration
    * @return
    *   a singular statistic.
    */
  def apply(duration: Long): Statistic = Statistic(1, duration)

  /** Monoid evidence for a statistic. */
  implicit val monoid: Monoid[Statistic] = Monoid.instance((a, b) => a + b, Statistic(0, 0))
end Statistic
