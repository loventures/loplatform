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

import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder, UserState, UserType}
import com.learningobjects.cpxp.util.PasswordUtils
import loi.cp.folder.{Folder, LightweightFolderService}
import loi.cp.item.LightweightItemService
import scalaz.\/
import scalaz.syntax.either.*
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*

@Service
class LightweightUserServiceImpl(
  folderService: LightweightFolderService,
  userDao: LightweightUserDao,
  itemService: LightweightItemService,
  timeSource: TimeSource,
  domain: => DomainDTO
) extends LightweightUserService:
  // For DB Testing
  def createUsersFolder(): Folder =
    folderService.createFolder(
      Some(UserFolderType.Users.itemName),
      Some("Users"),
      Some("user"),
      itemService.getDomainItem(domain)
    )

  def createOverlordUsersFolder(): Folder =
    folderService.createFolder(
      Some(UserFolderType.OverlordUsers.itemName),
      None,
      None,
      itemService.getDomainItem(domain)
    )
  // ///////////////

  private def acquireUsersFolderPessimisticLock(userFolderName: UserFolderType): UserFolder =
    val usersFolder: UserFolder = getFolder(userFolderName)
    folderService.acquirePessimisticLock(usersFolder.toFolder)
    usersFolder

  override def getUsersById(ids: Seq[Long]): Seq[UserDTO] =
    userDao.getUsers(ids).map(_.loadDto)

  override def getUsersByUserNames(userNames: Seq[String]): Seq[UserDTO] =
    val usersFolder: UserFolder = getFolder(UserFolderType.Users)
    userDao.getUsersByUserNames(userNames, usersFolder).map(_.loadDto)

  override def getUsersByExternalIds(externalIds: Seq[String]): Seq[UserDTO] =
    val usersFolder: UserFolder = getFolder(UserFolderType.Users)
    userDao.getUsersByExternalIds(externalIds, usersFolder).map(_.loadDto)

  override def createUser(
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
  ): UserCreationFailure \/ UserDTO =
    createUserInFolder(
      UserFolderType.Users,
      userName,
      emailAddress,
      externalId,
      userType,
      state,
      givenName,
      middleName,
      familyName,
      title,
      passphrase,
      subtenantId
    ).map(_.loadDto)

  override def getDomainUser(userType: DomainUserType): UserDTO =
    itemService
      .getNamedItem(userType.itemName)
      .flatMap(userItem => getUserById(userItem.getId))
      .getOrElse(throw new IllegalStateException(s"No domain user for ${userType.itemName}"))

  @VisibleForTesting
  def createDomainUser(prototype: DomainUserType): UserDTO =
    val userEntity: UserFinder =
      createUserInFolder(
        UserFolderType.DomainUsers,
        prototype.userName,
        None,
        None,
        prototype.userType,
        UserState.Active,
        prototype.displayName,
        None,
        "User",
        None,
        None,
        None
      ).valueOr(f => throw new IllegalArgumentException(s"Cannot create domain user: ${f.message}"))

    val userItem: Item = itemService.getItemReference(userEntity)
    itemService.setItemName(prototype.itemName, userItem)

    userEntity.loadDto
  end createDomainUser

  def validateNonEmpty[A <: UserCreationFailure](givenName: String, leftValue: => A): A \/ Unit =
    givenName.isEmpty.thenLeft(leftValue)

  private def createUserInFolder(
    folderName: UserFolderType,
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
  ): UserCreationFailure \/ UserFinder =
    // Lock before checking if the username is used
    val lockedUsersFolder: UserFolder = acquireUsersFolderPessimisticLock(folderName)

    val usersFolderItem: Item =
      itemService
        .findItem(lockedUsersFolder.id)
        .getOrElse(throw new IllegalStateException(s"No Users folder $folderName exists in domain"))

    // Salting, hashing etc
    val password: Option[String] = passphrase.map(phrase => PasswordUtils.encodePassword(domain, userName, phrase))

    for
      _ <- validateUniqueUserName(userName, lockedUsersFolder).widenl
      _ <- validateUniqueExternalId(externalId, lockedUsersFolder).widenl
      _ <- validateNonEmpty(givenName, EmptyGivenName).widenl
      _ <- validateNonEmpty(familyName, EmptyFamilyName)
    yield
      // Create the new user since we know it safely doesn't exist
      val entity: UserFinder =
        userDao
          .createUser(
            usersFolderItem,
            userName,
            emailAddress,
            externalId,
            userType,
            state,
            givenName,
            middleName.filter(_.nonEmpty),
            familyName,
            title,
            password,
            subtenantId,
            timeSource.instant
          )

      entity
    end for
  end createUserInFolder

  private def validateUniqueUserName(userName: String, folder: UserFolder): NonUniqueUserName \/ Unit =
    val existing: Option[UserFinder] = userDao.getUserByName(userName, folder)
    if existing.nonEmpty then NonUniqueUserName(userName).left
    else ().right

  private def validateUniqueExternalId(externalId: Option[String], folder: UserFolder): NonUniqueExternalId \/ Unit =
    val existing: Option[UserFinder] = externalId.flatMap(e => userDao.getUserByExternalId(e, folder))
    if existing.nonEmpty then NonUniqueExternalId(existing.get.externalId).left
    else ().right

  @VisibleForTesting
  def getFolder(name: UserFolderType): UserFolder =
    folderService
      .getFolder(name.itemName)
      .map(f => UserFolder(name, f))
      .getOrElse(throw new IllegalStateException(s"No Users folder exists in domain for $name"))
end LightweightUserServiceImpl
