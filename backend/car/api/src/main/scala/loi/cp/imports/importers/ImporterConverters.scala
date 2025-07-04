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

package loi.cp.imports.importers

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import scala.util.Try

trait ImporterConverters:

  /** Convert a valid ISO 8601 date string to Date object.
    */
  def stringToDate(dateStr: String): Try[Date] =
    Try({
      val fmt = DateTimeFormatter.ISO_DATE_TIME
      Date.from(fmt.parse[Instant](dateStr, Instant.from))
    })

  def dateToString(date: Date): String = date.toInstant.toString
end ImporterConverters
