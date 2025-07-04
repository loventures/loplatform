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

package loi.asset.license

import enumeratum.{ArgonautEnum, Enum, EnumEntry}

/** https://creativecommons.org/licenses/
  */

sealed abstract class License(override val entryName: String, val text: String) extends EnumEntry:
  lazy val abbreviation: String = entryName

object License extends Enum[License] with ArgonautEnum[License]:

  val values = findValues

  case object CC_BY       extends License("CC BY", "Attribution")
  case object CC_BY_SA    extends License("CC BY-SA", "Attribution-ShareAlike")
  case object CC_BY_ND    extends License("CC BY-ND", "Attribution-NoDerivs")
  case object CC_BY_NC    extends License("CC BY-NC", "Attribution-NonCommercial")
  case object CC_BY_NC_SA extends License("CC BY-NC-SA", "Attribution-NonCommercial-ShareAlike")
  case object CC_BY_NC_ND extends License("CC BY-NC-ND", "Attribution-NonCommercial-NoDerivs")
end License
