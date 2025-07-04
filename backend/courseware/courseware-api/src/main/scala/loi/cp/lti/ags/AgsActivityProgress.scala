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

package loi.cp.lti.ags

import enumeratum.{ArgonautEnum, CirceEnum, Enum, EnumEntry}

sealed trait AgsActivityProgress extends EnumEntry

object AgsActivityProgress
    extends Enum[AgsActivityProgress]
    with ArgonautEnum[AgsActivityProgress]
    with CirceEnum[AgsActivityProgress]:
  val values = findValues

  case object Initialized extends AgsActivityProgress

  case object Started extends AgsActivityProgress

  case object InProgress extends AgsActivityProgress

  case object Submitted extends AgsActivityProgress

  case object Completed extends AgsActivityProgress
end AgsActivityProgress
