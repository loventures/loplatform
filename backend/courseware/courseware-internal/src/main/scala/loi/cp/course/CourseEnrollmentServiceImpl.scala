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

package loi.cp.course

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.enrollment.{EnrollmentConstants, EnrollmentWebService}
import com.learningobjects.cpxp.service.query.{Comparison, Projection, QueryBuilder, Function as QBFunction}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFacade}
import loi.cp.role.RoleService

@Service
class CourseEnrollmentServiceImpl(
  enrollmentWebService: EnrollmentWebService,
  roleService: RoleService,
)(implicit cs: ComponentService)
    extends CourseEnrollmentService:
  import CourseEnrollmentServiceImpl.*

  override def areAllStudentsEnrolled(context: Long, students: Set[Long]): Boolean =
    getEnrolledQuery(context, StudentRôles*)
      .addCondition(
        DataTypes.META_DATA_TYPE_ID,
        Comparison.in,
        students
      )
      .getAggregateResult(QBFunction.COUNT) == students.size

  override def getEnrolledStudentIds(context: Long): List[Long] =
    getEnrolledQuery(context, StudentRôles*)
      .setProjection(Projection.ID)
      .getValues[Long]
      .toList

  override def getEnrolledStudentDTOs(context: Long): List[UserDTO] =
    getEnrolledQuery(context, StudentRôles*)
      .getFacades[UserFacade]
      .map(UserDTO.apply)
      .toList

  override def getEnrolledInstructorDTOs(context: Long): List[UserDTO] =
    getEnrolledQuery(context, EnrollmentWebService.INSTRUCTOR_ROLE_ID)
      .getFacades[UserFacade]
      .map(UserDTO.apply)
      .toList

  private def getEnrolledQuery(
    context: Long,
    roles: String*
  ): QueryBuilder =
    val qb = enrollmentWebService
      .getGroupEnrollmentsQuery(context, EnrollmentType.ACTIVE_ONLY)
      .addCondition(
        EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE,
        Comparison.in,
        roles.map(roleService.getRoleByRoleId).map(_.getId)
      )
    enrollmentWebService
      .getEnrollmentUsersQuery(
        qb,
        EnrollmentType.ALL
      )
  end getEnrolledQuery
end CourseEnrollmentServiceImpl

object CourseEnrollmentServiceImpl:
  val StudentRôles = List[String](
    EnrollmentWebService.STUDENT_ROLE_ID,
    EnrollmentWebService.TRIAL_LEARNER_ROLE_ID,
  )
