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

// TODO: I belong in `main` but `Group` is here and needs me
package com.learningobjects.de.enrollment

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.enrollment.EnrollmentComponent
import loi.cp.role.*
import scalaz.\/
import scalaz.syntax.functor.*
import scalaz.syntax.std.option.*

import scala.jdk.CollectionConverters.*

/** A default implementation of `EnrollmentOwner`.
  */
trait EnrollmentOwnerImpl extends EnrollmentOwner:
  import EnrollmentOwner.*

  protected def enrollmentWebService: EnrollmentWebService
  protected def supportedRoleService: SupportedRoleService
  protected implicit def componentService: ComponentService

  override def getEnrollments(userId: Long): Seq[EnrollmentComponent] =
    enrollmentWebService
      .getUserEnrollments(userId, this.getId)
      .asScala
      .toSeq
      .map(_.component[EnrollmentComponent])

  override def getSupportedRole(role: RoleComponent): Option[SupportedRole] =
    supportedRoleService.getRoles(this).asScala.collectFirst {
      case sr @ SupportedRole(_, RoleType(roleId, _, _), _) if roleId == role.getId => sr
    }

  override def enroll(
    user: UserDTO,
    role: RoleComponent,
    dataSource: String
  ): RoleNotSupportedError \/ EnrollmentComponent =
    getSupportedRole(role).toRightDisjunction(RoleNotSupportedError(RoleType(role))).as {
      enrollmentWebService
        .setSingleEnrollment(
          /*groupId    = */ this.getId,
          /*roleId     = */ role.getId,
          /*userId     = */ user.getId,
          /*dataSource = */ dataSource,
        )
        .component[EnrollmentComponent]
    }
end EnrollmentOwnerImpl
