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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.{UserDTO, UserState, UserType}
import scalaz.\/

/** A service for getting and creating user service objects from the database.
  */
@Service
trait LightweightUserService:

  /** Returns the user for the given id.
    *
    * @param id
    *   the id of the user
    * @return
    *   the user, if it exists
    */
  def getUserById(id: Long): Option[UserDTO] = getUsersById(Seq(id)).headOption

  /** Returns the users for the given ids. Any ids that do not map to a user will be ignored.
    *
    * @param ids
    *   the id of the user
    * @return
    *   the existing users
    */
  def getUsersById(ids: Seq[Long]): Seq[UserDTO]

  /** Returns the user with the provided user name field from the users folder. Matches will be done in a case
    * insensitive format. Users from the domain and overlord folders will not be searched.
    *
    * @param userName
    *   the user name of the user
    * @return
    *   the user with the provided user name, if it exists
    */
  def getUserByUserName(userName: String): Option[UserDTO] =
    getUsersByUserNames(Seq(userName)).headOption

  /** Returns the users with the provided user name fields from the users folder. Matches will be done in a case
    * insensitive format. Any user names that do not map to a user will be ignored. Users from the domain and overlord
    * folders will not be searched.
    *
    * @param userNames
    *   the user names of the users
    * @return
    *   the users with the provided user names
    */
  def getUsersByUserNames(userNames: Seq[String]): Seq[UserDTO]

  /** Returns the user with the provided external id field in the users folder. Users from the domain and overlord
    * folders will not be discovered.
    *
    * @param externalId
    *   the external id of the user
    * @return
    *   the user with the provided external id, if it exists
    */
  def getUserByExternalId(externalId: String): Option[UserDTO] =
    getUsersByExternalIds(Seq(externalId)).headOption

  /** Returns the users with the provided external id fields. Matches will be done in a case insensitive format. Any
    * external ids that do not map to a user will be ignored. Users from the domain and overlord folders will not be
    * discovered.
    *
    * @param externalIds
    *   the external ids of the users
    * @return
    *   the users with the provided external ids
    */
  def getUsersByExternalIds(externalIds: Seq[String]): Seq[UserDTO]

  /** Creates a new user in the users folder with the given values. If a user with the given {{userName}} exists, then
    * [[NonUniqueUserName]] is returned. If a user with a matching non-[[None]] external id exists, then a
    * [[NonUniqueExternalId]] is returned.
    *
    * @param userName
    *   the unique user name
    * @param emailAddress
    *   the email address of the user
    * @param externalId
    *   an identifier provided by an external system to identify the user
    * @param userType
    *   the type of user
    * @param state
    *   the initial state of the user
    * @param givenName
    *   the user's first name
    * @param middleName
    *   the user's last name
    * @param familyName
    *   the user's middle name
    * @param title
    *   the user's title
    * @param passphrase
    *   the passphrase for the user, if provided
    * @param subtenantId
    *   the subtenant for the user is under, if any
    * @return
    *   the created user; otherwise a failure
    */
  def createUser(
    userName: String,
    emailAddress: Option[String],
    externalId: Option[String],
    userType: UserType,
    state: UserState,
    givenName: String,
    middleName: Option[String],
    familyName: String,
    title: Option[String],
    passphrase: Option[String],
    subtenantId: Option[Long]
  ): UserCreationFailure \/ UserDTO

  /** Gets the singleton user of the provided prototype.
    *
    * @param userType
    *   the user prototype
    * @return
    *   the user for the provided prototype
    */
  def getDomainUser(userType: DomainUserType): UserDTO
end LightweightUserService
