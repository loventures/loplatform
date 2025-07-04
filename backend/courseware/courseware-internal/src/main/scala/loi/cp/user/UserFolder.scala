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

import com.learningobjects.cpxp.service.domain.DomainConstants
import com.learningobjects.cpxp.service.user.UserConstants
import enumeratum.{Enum, EnumEntry}
import loi.cp.folder.Folder

/** A service object for a folder for a cohort of users to exist under.
  *
  * @param id
  *   the pk of the folder
  * @param folderType
  *   the type of the cohort of the folder
  */
case class UserFolder(id: Long, folderType: UserFolderType):
  def toFolder = Folder(id, folderType.folderType)

object UserFolder:
  def apply(name: UserFolderType, folder: Folder): UserFolder =
    UserFolder(folder.id, name)

/** A type for a single cohort of users in a domain.
  *
  * @param itemName
  *   the singleton name of the folder in the item tree
  * @param folderType
  *   the type field for the base [[Folder]]
  */
sealed abstract class UserFolderType(val itemName: String, val folderType: Option[String]) extends EnumEntry

case object UserFolderType extends Enum[UserFolderType]:
  val values = findValues

  /** A folder of normal domain users */
  case object Users extends UserFolderType(UserConstants.ID_FOLDER_USERS, Some("user"))

  /** A folder of special case singleton users in the domain */
  case object DomainUsers extends UserFolderType(DomainConstants.FOLDER_ID_DOMAIN, Some("domain"))

  // This is roughly get or created by the overlord project or maybe bootstrapped from the overlord project
  // See OverlordDomainSudo
  /** a folder of overlorded users sudo'ed into the domain */
  case object OverlordUsers extends UserFolderType("folder-overlord-users", None)
end UserFolderType
