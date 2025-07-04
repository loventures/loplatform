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

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util as ju

final class InstantOps(private val self: Instant) extends AnyVal:

  /** Convert this [[Instant]] to a [[ju.Date Date]], truncating nanoseconds.
    *
    * @return
    *   a date object pretty close to this instant
    */
  def asDate: ju.Date = ju.Date.from(self)

  def asLocalDateTime: LocalDateTime = self.atZone(ZoneId.systemDefault).toLocalDateTime
end InstantOps

trait ToInstantOps:
  import language.implicitConversions

  @inline implicit final def ToInstantOps(self: Instant): InstantOps =
    new InstantOps(self)
