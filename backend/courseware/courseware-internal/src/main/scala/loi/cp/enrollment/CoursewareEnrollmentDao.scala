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

package loi.cp.enrollment

import java.time.Instant
import java.util.Date
import java.{lang, util as ju}

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentFinder
import com.learningobjects.cpxp.service.finder.Finder
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.relationship.{RelationshipConstants, RoleFinder, SupportedRoleFinder}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import com.learningobjects.cpxp.util.PersistenceIdFactory
import loi.cp.context.ContextId
import loi.cp.enrollment.EnrollmentDao.{EnrollmentResult, SupportedRoleResult}
import loi.cp.item.LightweightItemService
import org.hibernate.{LockMode, Session}
import scalaz.syntax.either.*
import scalaz.syntax.traverse.*
import scalaz.{NonEmptyList, \/}
import scaloi.GetOrCreate
import scaloi.syntax.CollectionBoxOps.*
import scaloi.syntax.collection.*

import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

/** A service for interacting with persisted rights and role entities
  */
@Service
trait CoursewareEnrollmentDao:
  def getRole(id: Long): Option[RoleFinder]

  def getRoles(ids: Seq[Long]): List[RoleFinder]

  def getRolesByName(names: RoleName*): List[RoleFinder]

  def getOrCreateRoles(names: Seq[RoleName]): List[RoleFinder]

  def getEnrollment(id: Long): Option[EnrollmentFinder]

  def getEnrollments(contexts: Seq[ContextId], users: NonEmptyList[Long]): List[EnrollmentResult]

  def getSupportedRoles(contextIds: Seq[ContextId]): List[SupportedRoleResult]

  def getOrCreateSupportedRole[F <: Finder](
    contextId: ContextId,
    role: Role
  )(implicit
    tt: ClassTag[F]
  ): SupportedRoleFinder

  def write(supportedRoleEntity: SupportedRoleFinder): SupportedRoleResult

  def addEnrollment(
    contextId: ContextId,
    user: UserDTO,
    role: Role,
    dataSource: Option[String],
    createDate: Instant,
    startDate: Option[Instant],
    endDate: Option[Instant]
  ): EnrollmentResult

  def deleteEnrollment(enrollment: ContextEnrollment, guid: String): NonExistentEnrollment \/ Unit
end CoursewareEnrollmentDao

@Service
class CoursewareEnrollmentDaoImpl(
  idFactory: PersistenceIdFactory,
  session: => Session,
  lightweightItemService: LightweightItemService,
  domain: => DomainDTO
) extends CoursewareEnrollmentDao:

  def getRole(id: Long): Option[RoleFinder] =
    Option(session.find[RoleFinder](classOf[RoleFinder], id))

  def getRoles(ids: Seq[Long]): List[RoleFinder] =
    session.bulkLoadFromCaches[RoleFinder](ids)

  def getRolesByName(names: RoleName*): List[RoleFinder] =
    session
      .createQuery[RoleFinder](
        s"""
         | FROM ${classOf[RoleFinder].getName}
         | WHERE del is null
         | AND root.id = :domain
         | AND roleId in (:names)
         """.stripMargin,
        classOf[RoleFinder]
      )
      .setParameter("domain", domain.id)
      .setParameter("names", names.map(_.persistenceKey).asJava)
      .getResultList
      .asScala
      .toList

  def getOrCreateRoles(names: Seq[RoleName]): List[RoleFinder] =
    val existingRoles: List[RoleFinder] = getRolesByName(names*)

    val foundRoleNames: List[RoleName] = existingRoles.map(entity => RoleName.of(entity))

    val missingRoleNames: Seq[RoleName] = names.diff(foundRoleNames)
    val roleParent: Item                =
      lightweightItemService
        .getNamedItem(RelationshipConstants.ID_FOLDER_ROLES)
        .getOrElse(throw new IllegalStateException("No Roles folder"))

    val addedRoles: Seq[RoleFinder]                 = addRoles(missingRoleNames, roleParent)
    val entityByRoleName: Map[RoleName, RoleFinder] =
      (existingRoles ++ addedRoles).groupUniqBy(entity => RoleName.of(entity))

    // Return the entities in order of the requested names
    names.map(entityByRoleName).toList
  end getOrCreateRoles

  def addRoles(roleNames: Seq[RoleName], parent: Item): Seq[RoleFinder] =
    val domainItem: Item = lightweightItemService.getDomainItem(domain)

    val entities: Seq[RoleFinder] =
      roleNames.map(roleName =>
        val newId: Long    = idFactory.generateId()
        val roleItem: Item =
          lightweightItemService.createItem(newId, RoleFinder.ITEM_TYPE_ROLE, parent, domainItem)

        val roleEntity = new RoleFinder
        roleEntity.setId(newId)
        roleEntity.roleId = roleName.persistenceKey

        val idStr =
          if roleName == StandardRoleName.TrialLearnerRole then "role-trial-learner"
          else s"role-${roleName.persistenceKey}"

        lightweightItemService.setItemName(idStr, roleItem)

        roleName match
          case CustomRoleName(_, display) => roleEntity.name = display
          case _                          => // no op
        LightweightItemService.setItemTreeProperties(roleEntity, roleItem, parent, domainItem)

        session.persist(roleEntity)
        roleEntity
      )

    session.flush()
    entities
  end addRoles

  def getEnrollment(id: Long): Option[EnrollmentFinder] =
    Option(session.find[EnrollmentFinder](classOf[EnrollmentFinder], id))

  def getEnrollments(contexts: Seq[ContextId], users: NonEmptyList[Long]): List[EnrollmentResult] =
    val queryString: String =
      s"""
         | FROM ${classOf[EnrollmentFinder].getName}
         | WHERE del is null
         | AND group.id in :contexts
         | AND parent.id in :userIds
       """.stripMargin

    session
      .createQuery(
        queryString,
        classOf[EnrollmentFinder]
      )
      .setParameter("contexts", contexts.map(c => Long.box(c.value)).asJavaCollection)
      .setParameter("userIds", users.toList.boxInsideTo[ju.List]())
      .getResultList
      .asScala
      .toList
      .map(entity =>
        // TODO: This shouldn't query/fetch, but does it?
        val userId: Long         = Long.unbox(session.getIdentifier(entity.parent()).asInstanceOf[lang.Long])
        val roleId: Long         = Long.unbox(session.getIdentifier(entity.role).asInstanceOf[lang.Long])
        val contextId: ContextId = ContextId(session.getIdentifier(entity.group).asInstanceOf[lang.Long])
        EnrollmentResult(entity, userId, roleId, contextId)
      )
      .sortBy(_.entity.id())
  end getEnrollments

  def getSupportedRoles(contextIds: Seq[ContextId]): List[SupportedRoleResult] =
    val entities: List[SupportedRoleFinder] =
      session
        .createQuery(
          s"""
           | FROM ${classOf[SupportedRoleFinder].getName}
           | WHERE del is null
           | AND parent.id in (:contexts)
         """.stripMargin,
          classOf[SupportedRoleFinder]
        )
        .setParameter("contexts", contextIds.map(id => Long.box(id.value)).asJavaCollection)
        .getResultList
        .asScala
        .toList

    entities.map(entity =>
      // TODO: This shouldn't query/fetch, but does it?
      val roleId: Long         = Long.unbox(session.getIdentifier(entity.role).asInstanceOf[lang.Long])
      val contextId: ContextId = ContextId(session.getIdentifier(entity.parent()).asInstanceOf[lang.Long])
      SupportedRoleResult(entity, roleId, contextId)
    )
  end getSupportedRoles

  def getOrCreateSupportedRole[F <: Finder](contextId: ContextId, role: Role)(implicit
    tt: ClassTag[F]
  ): SupportedRoleFinder =
    val domainItem: Item = lightweightItemService.getDomainItem(domain)

    val parentItem: Item =
      lightweightItemService
        .findItem(contextId.id)
        .getOrElse(throw new IllegalArgumentException(s"No Context for context ${contextId.id}"))

    val parentEntity: Finder = session.find(tt.runtimeClass.asInstanceOf[Class[F]], parentItem.getId)

    GetOrCreate[SupportedRoleFinder](
      () => getSupportedRoles(Seq(contextId)).find(_.roleId == role.id).map(_.entity),
      () => createSupportedRole(contextId, role, parentItem, domainItem),
      () => session.lock(parentEntity, LockMode.PESSIMISTIC_WRITE)
    ).result
  end getOrCreateSupportedRole

  private def createSupportedRole(contextId: ContextId, role: Role, parent: Item, domain: Item): SupportedRoleFinder =
    val roleEntity: RoleFinder =
      getRole(role.id).getOrElse(throw new IllegalStateException(s"No Role for role ${role.id}"))

    val entity = new SupportedRoleFinder
    entity.setId(idFactory.generateId())
    entity.role = roleEntity
    entity.setParent(parent)
    entity.setRoot(domain)

    session.flush()

    entity
  end createSupportedRole

  def write(supportedRoleEntity: SupportedRoleFinder): SupportedRoleResult =
    val updated: SupportedRoleFinder = session.merge(supportedRoleEntity).asInstanceOf[SupportedRoleFinder]

    // TODO: This shouldn't query/fetch, but does it?
    val roleId: Long         = Long.unbox(session.getIdentifier(updated.role).asInstanceOf[lang.Long])
    val contextId: ContextId = ContextId(session.getIdentifier(updated.parent()).asInstanceOf[lang.Long])
    SupportedRoleResult(updated, roleId, contextId)

  def addEnrollment(
    contextId: ContextId,
    user: UserDTO,
    role: Role,
    dataSource: Option[String],
    createDate: Instant,
    startDate: Option[Instant],
    endDate: Option[Instant]
  ): EnrollmentResult =
    val domainItem: Item = lightweightItemService.getDomainItem(domain)

    val userItem: Item =
      lightweightItemService
        .findItem(user.id)
        .getOrElse(throw new IllegalArgumentException(s"No User for user ${user.id}"))

    val contextItem: Item =
      lightweightItemService
        .findItem(contextId.value)
        .getOrElse(throw new IllegalArgumentException(s"No Group for group ${contextId.value}"))

    val roleEntity: RoleFinder =
      getRole(role.id).getOrElse(throw new IllegalStateException(s"No existing entity for $role"))

    val enrollmentEntity: EnrollmentFinder = new EnrollmentFinder
    val newId: Long                        = idFactory.generateId()
    enrollmentEntity.setId(newId)
    enrollmentEntity.group = contextItem

    enrollmentEntity.role = roleEntity
    dataSource.foreach(source => enrollmentEntity.dataSource = source)

    enrollmentEntity.disabled = false

    enrollmentEntity.startTime = startDate.map(Date.from).orNull
    enrollmentEntity.stopTime = endDate.map(Date.from).orNull
    enrollmentEntity.createdOn = Date.from(createDate)

    LightweightItemService.setItemTreeProperties(enrollmentEntity, userItem, domainItem)

    session.persist(enrollmentEntity)
    session.flush()

    EnrollmentResult(enrollmentEntity, userItem.getId, role.id, ContextId(contextItem.getId))
  end addEnrollment

  def deleteEnrollment(enrollment: ContextEnrollment, guid: String): NonExistentEnrollment \/ Unit =
    val possibleEntity: Option[EnrollmentFinder] = Option(session.find(classOf[EnrollmentFinder], enrollment.id))

    possibleEntity
      .map(entity =>
        session
          .createMutationQuery(s"""
                      | UPDATE ${classOf[EnrollmentFinder].getName}
                      | SET del = '$guid'
                      | WHERE id = ${entity.id()}
           """.stripMargin)
          .executeUpdate()

        session.flush()

        // TODO: Any legacy cache invalidation

        ().right[NonExistentEnrollment]
      )
      .getOrElse({
        NonExistentEnrollment(enrollment.user, enrollment.context.value).left
      })
  end deleteEnrollment
end CoursewareEnrollmentDaoImpl

object EnrollmentDao:
  case class EnrollmentResult(entity: EnrollmentFinder, userId: Long, roleId: Long, contextId: ContextId)
  case class SupportedRoleResult(entity: SupportedRoleFinder, roleId: Long, contextId: ContextId)
