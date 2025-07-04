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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.course.lightweight.Lwc
import loi.cp.right.Right

/** A service to get and set the roles and enrollments for domains and sections.
  */
@Service
trait LightweightEnrollmentService:

  /** Returns the [[Role]] for the given [[RoleName]] in the given domain.
    *
    * @param roleName
    *   the name of the role
    * @return
    *   the [[Role]] for the [[RoleName]]
    */
  def getRoleByName(roleName: RoleName): Role =
    getRolesByName(roleName).headOption
      .getOrElse(throw new IllegalArgumentException(s"No such role: ${roleName.persistenceKey}"))

  /** Returns the [[Role]] s for the given [[RoleName]] s in the given domain.
    *
    * @param roleNames
    *   the name of the roles
    * @return
    *   the [[Role]] s for the [[RoleName]] s
    */
  def getRolesByName(roleNames: RoleName*): Seq[Role]

  /** Returns any existing [[Role]] for the [[RoleName]], or creates it if it does not exist
    *
    * @param roleNames
    *   the names of the roles
    * @return
    *   the new or existing [[RoleName]] s
    */
  def getOrCreateRoles(roleNames: RoleName*): Seq[Role]

  /** Gets a user's domain enrollments, if any exists.
    *
    * @param domain
    *   the domain to search for enrollments in
    * @param user
    *   the user to search for enrollments for
    * @return
    *   the user enrollments, if they exist
    */
  def getDomainEnrollment(domain: DomainDTO, user: UserDTO): Seq[ContextEnrollment]

  /** Gets a user's domain enrollment and the rights granted by those enrollments, if it exists.
    *
    * @param domain
    *   the domain to search for enrollments and rights in
    * @param user
    *   the user to search for enrollments and rights in
    * @return
    *   the user enrollment and domain rights, if they exist
    */
  def getDomainRights(domain: DomainDTO, user: UserDTO): Seq[DomainRights]

  /** Creates a domain enrollments for a user.
    *
    * @param domain
    *   the domain to add an enrollment to
    * @param user
    *   the user to add an enrollment for
    * @param role
    *   the role to grant the user
    * @param dataSource
    *   the source the enrollment is coming from
    * @param startDate
    *   the time the enrollment starts be active, if constrained by a start date
    * @param endDate
    *   the time the enrollment stops being active, if constrained by an end date
    * @return
    *   the new enrollment and what rights it grants
    */
  def addDomainEnrollment(
    domain: DomainDTO,
    user: UserDTO,
    role: Role,
    dataSource: Option[String],
    startDate: Option[Instant],
    endDate: Option[Instant]
  ): ContextEnrollment

  /** Returns an enrollment with rights in a section, if the user is enrolled. Domain right will also be included in the
    * returned value. Inactive enrollments will be returned and must be filtered out by the caller, if necessary.
    *
    * @param user
    *   the user to search for enrollments for
    * @param section
    *   the section to search for enrollments for.
    * @return
    *   the enrollment and rights in a section with domain; otherwise, [[None]] if unenrolled
    */
  def getEnrollment(section: Lwc, user: UserDTO): Seq[ContextEnrollment]

  /** Gets a user's domain and section enrollments and the rights granted by those enrollments, if it exists.
    *
    * @param section
    *   the section to search for enrollments in
    * @param user
    *   the user to search for enrollments for
    * @return
    *   the user enrollment for the section and domain and the rights for those enrollments, if they exist
    */
  def getRights(section: Lwc, user: UserDTO): (Seq[DomainRights], Seq[SectionRights])

  /** Adds an enrollment for a given user in a section. This does not alter any existing enrollments in the given
    * section.
    *
    * @param section
    *   the search to add the enrollment for
    * @param user
    *   the user to add the enrollment for
    * @param role
    *   the role to grant
    * @return
    *   the new rights granted in the course for the enrollment
    */
  def addEnrollment(
    section: Lwc,
    user: UserDTO,
    role: Role,
    dataSource: Option[String],
    startDate: Option[Instant],
    endDate: Option[Instant]
  ): ContextEnrollment

  /** Deletes a domain or section enrollment.
    *
    * @param enrollment
    *   the enrollment to delete
    */
  def deleteEnrollment(enrollment: ContextEnrollment): Unit

  /** Assigns a set of rights to a given role in a domain.
    *
    * @param domain
    *   the domain to grant rights to a role holder
    * @param role
    *   the role to grant rights to
    * @param grantedRights
    *   the rights to grant the role holder
    * @param deniedRights
    *   the rights to deny the role holder
    * @return
    *   the new or updated [[SupportedRole]] entry
    */
  def setSupportedDomainRole(
    domain: DomainDTO,
    role: Role,
    grantedRights: Seq[Class[? <: Right]],
    deniedRights: Seq[Class[? <: Right]]
  ): SupportedRole

  /** Assigns a set of rights to a given role in a section.
    *
    * @param section
    *   the section to grant rights to a role holder
    * @param role
    *   the role to grant rights to
    * @param grantedRights
    *   the rights to grant the role holder
    * @param deniedRights
    *   the rights to deny the role holder
    * @return
    *   the new or updated [[SupportedRole]] entry
    */
  def setSupportedRole(
    section: Lwc,
    role: Role,
    grantedRights: Seq[Class[? <: Right]],
    deniedRights: Seq[Class[? <: Right]]
  ): SupportedRole
end LightweightEnrollmentService
