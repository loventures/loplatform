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

import java.time.Instant
import java.util.Date

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.subtenant.SubtenantFinder
import com.learningobjects.cpxp.service.user.{UserConstants, UserFinder, UserState, UserType}
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import com.learningobjects.cpxp.util.PersistenceIdFactory
import loi.cp.item.LightweightItemService
import org.hibernate.Session

import scala.jdk.CollectionConverters.*

/** A service for interacting with persisted user entities.
  */
@Service
trait LightweightUserDao:
  def getUser(id: Long): Option[UserFinder]

  def getUsers(ids: Seq[Long]): Seq[UserFinder]

  def createUser(
    folderItem: Item,
    userName: String,
    emailAddress: Option[String],
    externalId: Option[String],
    userType: UserType,
    state: UserState,
    givenName: String,
    middleName: Option[String],
    familyName: String,
    title: Option[String],
    password: Option[String],
    subtenantId: Option[Long],
    createTime: Instant
  ): UserFinder

  def getUserByName(userName: String, userFolder: UserFolder): Option[UserFinder]

  def getUsersByUserNames(userNames: Seq[String], userFolder: UserFolder): Seq[UserFinder]

  def getUserByExternalId(externalId: String, userFolder: UserFolder): Option[UserFinder]

  def getUsersByExternalIds(externalIds: Seq[String], userFolder: UserFolder): Seq[UserFinder]
end LightweightUserDao

@Service
class LightweightUserDaoImpl(
  idFactory: PersistenceIdFactory,
  lightweightItemService: LightweightItemService,
  session: => Session,
  domain: => DomainDTO
) extends LightweightUserDao:
  def getUser(id: Long): Option[UserFinder] =
    getUsers(Seq(id)).headOption

  def getUsers(ids: Seq[Long]): Seq[UserFinder] =
    session
      .bulkLoadFromCaches[UserFinder](ids)
      .filter(entity =>
        // TODO: This shouldn't query/fetch, but does it?
        val rootId: Long = entity.root().getId

        entity.del() == null && rootId == domain.id
      )

  def createUser(
    folderItem: Item,
    userName: String,
    emailAddress: Option[String],
    externalId: Option[String],
    userType: UserType,
    state: UserState,
    givenName: String,
    middleName: Option[String],
    familyName: String,
    title: Option[String],
    password: Option[String],
    subtenantId: Option[Long],
    createTime: Instant
  ): UserFinder =
    val domainItem: Item =
      lightweightItemService.getDomainItem(domain)

    val possibleSubtenant: Option[SubtenantFinder] =
      subtenantId.map(id => session.getReference(classOf[SubtenantFinder], id))

    val newId: Long    = idFactory.generateId()
    val userItem: Item =
      lightweightItemService.createItem(newId, UserConstants.ITEM_TYPE_USER, folderItem, domainItem)

    val entity = new UserFinder
    entity.setId(newId)
    entity.createTime = Date.from(createTime)
    entity.userName = userName
    entity.utype = userType.name()
    emailAddress.foreach(email => entity.emailAddress = email)
    externalId.foreach(external => entity.externalId = external)
    entity.givenName = givenName
    middleName.foreach(m => entity.middleName = m)
    entity.familyName = familyName
    title.foreach(t => entity.title = t)
    password.foreach(pw => entity.password = pw)
    possibleSubtenant.foreach(subtenant => entity.subtenant = subtenant)

    setState(entity, state)

    val fullName: String =
      Option(givenName).getOrElse("") + " " + middleName.getOrElse("") + " " + Option(familyName).getOrElse("")
    entity.fullName = fullName

    LightweightItemService.setItemTreeProperties(entity, userItem, folderItem, domainItem)

    session.persist(entity)

    entity
  end createUser

  private def setState(entity: UserFinder, state: UserState): Unit =
    entity.state = state.name()
    entity.disabled = state.getDisabled
    entity.inDirectory = state.getInDirectory

  def getUserByName(userName: String, userFolder: UserFolder): Option[UserFinder] =
    getUsersByUserNames(Seq(userName), userFolder).headOption

  def getUsersByUserNames(userNames: Seq[String], userFolder: UserFolder): Seq[UserFinder] =
    val lowerCased: Seq[String] = userNames.map(_.toLowerCase)

    val domainItem: Item = session.getReference(classOf[Item], domain.id)
    val folderItem: Item = session.getReference(classOf[Item], userFolder.id)

    session
      .createQuery(
        """
          | FROM UserFinder
          | WHERE del is null
          | AND root = :domain
          | AND parent = :folder
          | AND LOWER(userName) in :userNames
        """.stripMargin,
        classOf[UserFinder]
      )
      .setParameter("domain", domainItem)
      .setParameter("folder", folderItem)
      .setParameter("userNames", lowerCased.asJava)
      .getResultList
      .asScala
      .toSeq
  end getUsersByUserNames

  def getUserByExternalId(externalId: String, userFolder: UserFolder): Option[UserFinder] =
    getUsersByExternalIds(Seq(externalId), userFolder).headOption

  def getUsersByExternalIds(externalIds: Seq[String], userFolder: UserFolder): Seq[UserFinder] =
    val lowerCased: Seq[String] = externalIds.map(_.toLowerCase)

    val domainItem: Item = session.getReference(classOf[Item], domain.id)
    val folderItem: Item = session.getReference(classOf[Item], userFolder.id)

    session
      .createQuery(
        """
          | FROM UserFinder
          | WHERE del is null
          | AND root = :domain
          | AND parent = :folder
          | AND LOWER(externalId) in :externalIds
        """.stripMargin,
        classOf[UserFinder]
      )
      .setParameter("domain", domainItem)
      .setParameter("folder", folderItem)
      .setParameter("externalIds", lowerCased.asJava)
      .getResultList
      .asScala
      .toSeq
  end getUsersByExternalIds
end LightweightUserDaoImpl
