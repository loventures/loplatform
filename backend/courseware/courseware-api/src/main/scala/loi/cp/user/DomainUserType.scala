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

package loi.cp.user

import com.learningobjects.cpxp.service.user.{UserConstants, UserType}
import enumeratum.{Enum, EnumEntry}

/** A prototype of a domain user.
  *
  * @param itemName
  *   the singleton name of the user in the item system
  * @param userName
  *   the user name
  * @param displayName
  *   the display name of the user (given name)
  * @param userType
  *   the type of the user
  */
sealed abstract class DomainUserType(
  val itemName: String,
  val userName: String,
  val displayName: String,
  val userType: UserType
) extends EnumEntry

object DomainUserType extends Enum[DomainUserType]:
  val values = findValues

  case object Root    extends DomainUserType(UserConstants.ID_USER_ROOT, "root", "Administrator", UserType.Overlord)
  case object Unknown extends DomainUserType(UserConstants.ID_USER_UNKNOWN, "unknown", "Unknown", UserType.Unknown)
  case object Anonymous
      extends DomainUserType(UserConstants.ID_USER_ANONYMOUS, "anonymous", "Anonymous", UserType.Anonymous)
