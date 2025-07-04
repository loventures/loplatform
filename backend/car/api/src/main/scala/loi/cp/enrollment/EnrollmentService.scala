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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.enrollment.EnrollmentFacade
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.de.group.GroupComponent
import loi.cp.role.RoleType

import java.util.Date

// EnrollmentWebService but in scala and with analytics
@Service
trait EnrollmentService:

  def loadEnrollments(
    userId: Long,
    groupId: Long,
    enrollmentType: EnrollmentType
  ): List[EnrollmentFacade]

  /** Ensures that there is one enrollment for the (user, group) with the given attributes. If no such enrollment
    * exists, one is created. Otherwise, one enrollment is updated and any others are deleted.
    */
  def setEnrollment(
    user: UserId,
    group: GroupComponent,
    role: RoleType,
    dataSource: Option[String] = None,
    startTime: Option[Date] = None,
    stopTime: Option[Date] = None,
    disabled: Boolean = false,
  ): EnrollmentComponent

  def updateEnrollment(
    enrollment: EnrollmentComponent,
    role: RoleType,
    dataSource: Option[String],
    startTime: Option[Date],
    stopTime: Option[Date],
    disabled: Boolean
  ): Unit

  def deleteEnrollment(id: Long): Unit

  def transferEnrollment(enrollment: EnrollmentComponent, destinationId: Long): EnrollmentComponent
end EnrollmentService
