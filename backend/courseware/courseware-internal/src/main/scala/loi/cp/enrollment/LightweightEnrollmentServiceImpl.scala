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

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFinder}
import com.learningobjects.cpxp.service.finder.Finder
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.relationship.{RoleFinder, SupportedRoleFinder}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.GuidUtil
import loi.cp.context.ContextId
import loi.cp.course.lightweight.Lwc
import loi.cp.enrollment.EnrollmentDao.{EnrollmentResult, SupportedRoleResult}
import loi.cp.right.Right
import org.log4s.Logger
import scalaz.NonEmptyList
import scaloi.misc.TimeSource
import scaloi.syntax.collection.*

import scala.reflect.ClassTag

@Service
class LightweightEnrollmentServiceImpl(
  enrollmentDao: CoursewareEnrollmentDao,
  timeSource: TimeSource,
  user: => UserDTO,
  domain: => DomainDTO
) extends LightweightEnrollmentService:
  import LightweightEnrollmentServiceImpl.*

  private def getRoles(roleIds: Seq[Long]): Seq[Role] =
    enrollmentDao.getRoles(roleIds).map(toRole)

  override def getRolesByName(roleNames: RoleName*): Seq[Role] =
    val roles: List[Role] = enrollmentDao.getRolesByName(roleNames*).map(toRole)
    sorted(roleNames, roles)

  override def getOrCreateRoles(roleNames: RoleName*): Seq[Role] =
    val roles: List[Role] = enrollmentDao.getOrCreateRoles(roleNames).map(toRole)
    sorted(roleNames, roles)

  private def sorted(roleNames: Seq[RoleName], roles: Seq[Role]): Seq[Role] =
    val nameToRole: Map[RoleName, Role] = roles.groupUniqBy(_.roleName)
    roleNames.flatMap(nameToRole.get)

  private def getEnrollmentResultsAndRoles(
    contextId: ContextId,
    user: UserDTO
  ): (List[EnrollmentResult], List[Role]) =
    val enrollmentResults: List[EnrollmentResult] =
      enrollmentDao.getEnrollments(Seq(contextId), NonEmptyList(user.id))

    val roles: List[Role] = getRoles(enrollmentResults.map(_.roleId).distinct).toList

    (enrollmentResults, roles)
  end getEnrollmentResultsAndRoles

  private def getEnrollment(contextId: ContextId, user: UserDTO): List[ContextEnrollment] =
    val (enrollmentResults, roles) = getEnrollmentResultsAndRoles(contextId, user)

    val rolesById: Map[Long, Role] = roles.groupUniqBy(_.id)
    enrollmentResults.map(buildEnrollment(_, rolesById))

  private def getEnrollmentAndRights(contextId: ContextId, user: UserDTO): List[(ContextEnrollment, RightsSet)] =
    val (enrollmentResults, roles) = getEnrollmentResultsAndRoles(contextId, user)
    val rolesById: Map[Long, Role] = roles.groupUniqBy(_.id)

    val supportedRoleResults: List[SupportedRoleResult] =
      enrollmentDao.getSupportedRoles(Seq(ContextId(domain.id)))
    val supportedRoles: List[SupportedRole]             =
      supportedRoleResults.map(buildSupportedRole(_, rolesById))

    enrollmentResults.map(enrollmentRow =>
      val enrollment: ContextEnrollment = buildEnrollment(enrollmentRow, rolesById)
      val rights: RightsSet             = buildRights(enrollment, supportedRoles)
      (enrollment, rights)
    )
  end getEnrollmentAndRights

  override def getDomainEnrollment(
    domain: DomainDTO,
    user: UserDTO
  ): Seq[ContextEnrollment] =
    getEnrollment(ContextId(domain.id), user)

  override def getDomainRights(
    domain: DomainDTO,
    user: UserDTO
  ): Seq[DomainRights] =
    getEnrollmentAndRights(ContextId(domain.id), user).map({ case (enrollment, rights) =>
      DomainRights(enrollment, rights)
    })

  override def addDomainEnrollment(
    domain: DomainDTO,
    user: UserDTO,
    role: Role,
    dataSource: Option[String],
    startDate: Option[Instant],
    endDate: Option[Instant]
  ): ContextEnrollment =
    val row: EnrollmentResult =
      enrollmentDao.addEnrollment(ContextId(domain.id), user, role, dataSource, timeSource.instant, startDate, endDate)

    buildEnrollment(row, Map(role.id -> role))
  end addDomainEnrollment

  override def getEnrollment(
    section: Lwc,
    user: UserDTO
  ): Seq[ContextEnrollment] =
    getEnrollment(ContextId(section.id), user)

  override def getRights(
    section: Lwc,
    user: UserDTO
  ): (Seq[DomainRights], Seq[SectionRights]) =
    val sectionId                              = ContextId(section.id)
    val domainId                               = ContextId(domain.id)
    val enrollmentRows: List[EnrollmentResult] =
      enrollmentDao.getEnrollments(Seq(sectionId, domainId), NonEmptyList(user.id))

    val allRoles: Seq[Role]       = getRoles(enrollmentRows.map(_.roleId))
    val roleById: Map[Long, Role] = allRoles.groupUniqBy(_.id)

    val allSupportedRoles: Seq[SupportedRole] =
      enrollmentDao
        .getSupportedRoles(Seq(sectionId, domainId))
        .filter(row => roleById.contains(row.roleId)) // Only build the supported roles for roles we actually have
        .map(buildSupportedRole(_, roleById))

    val domainEnrollments: List[ContextEnrollment] =
      enrollmentRows
        .filter(_.contextId == domainId)
        .map(buildEnrollment(_, roleById))

    val domainRights: List[DomainRights] =
      domainEnrollments.map(enrollment =>
        val supportedDomainRoles: Seq[SupportedRole] = allSupportedRoles.filter(_.contextId == enrollment.context)
        val rights: RightsSet                        = buildRights(enrollment, supportedDomainRoles)
        DomainRights(enrollment, rights)
      )

    val sectionEnrollments: List[ContextEnrollment] =
      enrollmentRows
        .filter(_.contextId == sectionId)
        .map(buildEnrollment(_, roleById))

    val sectionRights: List[SectionRights] =
      sectionEnrollments.map(enrollment =>
        val supportedSectionRoles: Seq[SupportedRole] = allSupportedRoles.filter(_.contextId == enrollment.context)
        val rights: RightsSet                         = buildRights(enrollment, supportedSectionRoles)
        SectionRights(enrollment, rights)
      )

    (domainRights, sectionRights)
  end getRights

  override def addEnrollment(
    section: Lwc,
    user: UserDTO,
    role: Role,
    dataSource: Option[String],
    startDate: Option[Instant],
    endDate: Option[Instant]
  ): ContextEnrollment =
    val row: EnrollmentResult =
      enrollmentDao.addEnrollment(section.contextId, user, role, dataSource, timeSource.instant, startDate, endDate)

    buildEnrollment(row, Map(role.id -> role))
  end addEnrollment

  override def deleteEnrollment(enrollment: ContextEnrollment): Unit =
    val guid: String = GuidUtil.deleteGuid(Date.from(timeSource.instant), user.id)
    logger.info(s"Deleting enrollment ${enrollment.id} using guid $guid")
    enrollmentDao.deleteEnrollment(enrollment, guid)

  override def setSupportedDomainRole(
    domain: DomainDTO,
    role: Role,
    grantedRights: Seq[Class[? <: Right]],
    deniedRights: Seq[Class[? <: Right]]
  ): SupportedRole =
    setSupportedRole[DomainFinder](ContextId(domain.id), role, grantedRights, deniedRights)

  override def setSupportedRole(
    section: Lwc,
    role: Role,
    grantedRights: Seq[Class[? <: Right]],
    deniedRights: Seq[Class[? <: Right]]
  ): SupportedRole =
    setSupportedRole[GroupFinder](section.contextId, role, grantedRights, deniedRights)

  private def setSupportedRole[F <: Finder](
    contextId: ContextId,
    role: Role,
    grantedRights: Seq[Class[? <: Right]],
    deniedRights: Seq[Class[? <: Right]]
  )(implicit tt: ClassTag[F]): SupportedRole =
    val supportedRoleEntity: SupportedRoleFinder = enrollmentDao.getOrCreateSupportedRole[F](contextId, role)

    val rights: Seq[EnrollmentRight] = GrantRight(grantedRights*) ++ DenyRight(deniedRights*)
    supportedRoleEntity.rights = rights.toList.asJson.nospaces

    val result: SupportedRoleResult = enrollmentDao.write(supportedRoleEntity)
    buildSupportedRole(result, Map(role.id -> role))
  end setSupportedRole
end LightweightEnrollmentServiceImpl

object LightweightEnrollmentServiceImpl:
  val logger: Logger = org.log4s.getLogger

  private def buildEnrollment(row: EnrollmentResult, rolesById: Map[Long, Role]): ContextEnrollment =
    val role: Role =
      rolesById.getOrElse(
        row.roleId,
        throw new IllegalStateException(s"No known role for id ${row.roleId} for enrollment ${row.entity.id()}")
      )

    ContextEnrollment(
      row.entity.id(),
      row.contextId,
      row.userId,
      role,
      row.entity.disabled,
      Option(row.entity.startTime).map(_.toInstant),
      Option(row.entity.stopTime).map(_.toInstant),
      Option(row.entity.dataSource),
    )
  end buildEnrollment

  private def buildSupportedRole(row: SupportedRoleResult, rolesById: Map[Long, Role]): SupportedRole =
    val role: Role                    = rolesById(row.roleId)
    val rights: List[EnrollmentRight] =
      row.entity.rights
        .decodeOption[List[EnrollmentRight]]
        .getOrElse(
          throw new IllegalStateException(
            s"Unreadable supported role rights for ${row.entity.id()}: ${row.entity.rights}"
          )
        )

    SupportedRole(row.entity.id(), row.contextId, role, RightsSet(rights.toSet))
  end buildSupportedRole

  private def buildRights(enrollment: ContextEnrollment, supportedRoles: Seq[SupportedRole]): RightsSet =
    val supportedRole: Option[SupportedRole] =
      supportedRoles.find(supported =>
        enrollment.context.equals(supported.contextId) && enrollment.role.equals(supported.role)
      )

    supportedRole.map(_.rights).getOrElse(RightsSet.empty)

  private def toRole(entity: RoleFinder): Role =
    Role(entity.id(), RoleName(entity.roleId, Option(entity.name)))
end LightweightEnrollmentServiceImpl
