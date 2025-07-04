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

import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.relationship.RoleFinder
import enumeratum.{Enum, EnumEntry}
import loi.cp.context.ContextId
import scaloi.syntax.collection.*

/** A role that can be given to a user by way of an [[ContextEnrollment]].
  *
  * @param id
  *   the persistence id of the role
  * @param roleName
  *   the name of the role
  */
case class Role(id: Long, roleName: RoleName)

/** A name that identifies a role.
  *
  * @param persistenceKey
  *   the lookup key to discover the role in a domain (referred to as roleId in the database)
  */
sealed abstract class RoleName(val persistenceKey: String)

object RoleName:
  def of(roleEntity: RoleFinder): RoleName = RoleName(roleEntity.roleId, Option(roleEntity.name))

  def apply(persistenceKey: String, display: Option[String]): RoleName =
    StandardRoleName.roleNameByPersistenceKey
      .getOrElse(persistenceKey, CustomRoleName(persistenceKey, display.getOrElse("")))

/** A role that was created on domain.
  *
  * @param persistenceKey
  *   the lookup key to discover the role in a domain
  * @param display
  *   the display name of the role
  */
case class CustomRoleName(override val persistenceKey: String, display: String) extends RoleName(persistenceKey)

/** The name of a predefined role in the system.
  *
  * @param persistenceKey
  *   the lookup key to discover the role in a domain
  */
sealed abstract class StandardRoleName(persistenceKey: String) extends RoleName(persistenceKey) with EnumEntry

object StandardRoleName extends Enum[StandardRoleName]:
  val values                   = findValues
  val roleNameByPersistenceKey = values.groupUniqBy(_.persistenceKey)

  case object StudentRole           extends StandardRoleName(EnrollmentWebService.STUDENT_ROLE_ID)
  case object StaffRole             extends StandardRoleName(EnrollmentWebService.STAFF_ROLE_ID)
  case object GuestRole             extends StandardRoleName(EnrollmentWebService.GUEST_ROLE_ID)
  case object HostingAdminRole      extends StandardRoleName(EnrollmentWebService.HOSTING_ADMIN_ROLE_ID)
  case object HostingSupportRole    extends StandardRoleName(EnrollmentWebService.HOSTING_SUPPORT_ROLE_ID)
  case object AdministratorRole     extends StandardRoleName(EnrollmentWebService.ADMINISTRATOR_ROLE_ID)
  case object InstructorRole        extends StandardRoleName(EnrollmentWebService.INSTRUCTOR_ROLE_ID)
  case object AdvisorRole           extends StandardRoleName(EnrollmentWebService.ADVISOR_ROLE_ID)
  case object TrialLearnerRole      extends StandardRoleName(EnrollmentWebService.TRIAL_LEARNER_ROLE_ID)
  case object FacultyRole           extends StandardRoleName(EnrollmentWebService.FACULTY_ROLE_ID)
  case object ProvisioningAdminRole extends StandardRoleName(EnrollmentWebService.PROVISIONING_ADMIN_ROLE_ID)
end StandardRoleName

/** The usage of a role in a context (domain or course). This grants and denies a set of rights to a particular role.
  *
  * @param id
  *   the persistence id of this record
  * @param contextId
  *   what context this supported role applies to
  * @param role
  *   the role for the context
  * @param rights
  *   the rights grants and denials for the role in the context
  */
case class SupportedRole(id: Long, contextId: ContextId, role: Role, rights: RightsSet)

object SupportedRole:
  import StandardRoleName.*
  import loi.cp.right.StandardRight.*

  /** The default policies for supported roles for rights in a domain.
    */
  val defaultDomainRightsByRoleName: Map[RoleName, RightsSet] =
    Map(
      AdministratorRole     -> RightsSet.of(GrantRight(Admin, ConfigureLtiCourse)*),
      FacultyRole           -> RightsSet.of(GrantRight(ConfigureLtiCourse)),
      StudentRole           -> RightsSet.of(),
      HostingAdminRole      -> RightsSet.of(GrantRight(Admin, AllOverlord, ConfigureLtiCourse)*),
      StaffRole             -> RightsSet.of(GrantRight(Admin, Underlord, ConfigureLtiCourse)*),
      HostingSupportRole    -> RightsSet.of(
        GrantRight(Admin, Support, ConfigureLtiCourse, AnnouncementAdmin, ConfigurationAdmin)*
      ),
      ProvisioningAdminRole -> RightsSet.of(
        GrantRight(CourseAdmin, ProjectAdmin, ConfigureLtiCourse, ManageLibrariesRead)*
      )
    )

  // From [[CoursePermissions]]
  /** The default policies for supported roles for rights in a section.
    */
  val defaultSectionRightsByRoleName: Map[RoleName, RightsSet] =
    Map(
      StudentRole      -> RightsSet.of(GrantRight(LearnCourse)),
      InstructorRole   -> RightsSet.of(GrantRight(TeachCourse)),
      AdvisorRole      -> RightsSet.of(GrantRight(ViewGrade, ContentCourse, CourseRoster)*),
      TrialLearnerRole -> RightsSet.of(GrantRight(LearnCourse), DenyRight(FullContent))
    )
end SupportedRole
