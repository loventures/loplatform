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

package com.learningobjects.de.enrollment

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.enrollment.EnrollmentOwner.RoleNotSupportedError
import loi.cp.enrollment.EnrollmentComponent
import loi.cp.role.{RoleComponent, RoleType, SupportedRole}
import scalaz.\/

/** An entity which can have users enrolled within.
  *
  * Clumsily extracted from `GroupComponent` to help make it less ubiquitous.
  */
trait EnrollmentOwner extends ComponentInterface with Id:

  /** Fetch enrollments for user.
    */
  // the SRSity of this method is predicted to decrease substantially in 2018
  @RequestMapping(path = "enrollments/{userId}", method = Method.GET)
  def getEnrollments(@PathVariable("userId") userId: Long): Seq[EnrollmentComponent]

  /** If `role` is supported, return a `SupportedRole` containing information about that role's specific rights in this
    * context.
    */
  def getSupportedRole(role: RoleComponent): Option[SupportedRole]

  def enroll(
    user: UserDTO,
    role: RoleComponent,
    dataSource: String,
  ): RoleNotSupportedError \/ EnrollmentComponent
end EnrollmentOwner

object EnrollmentOwner:

  final case class RoleNotSupportedError(role: RoleType):
    def message = s"Role ${role.name} (${role.roleId}) is not supported"
