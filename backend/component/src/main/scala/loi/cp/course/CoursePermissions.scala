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

import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import loi.cp.course.right.*
import loi.cp.right.Right
import loi.cp.role.RoleService

private[course] object CoursePermissions:

  // i feel such sorrow and shame, these aren't supposed to be in the section
  // they are supposed to be centrally managed

  def initPermissions(course: CourseComponent)(implicit rs: RoleService): Unit =
    if rs.getSupportedRoles(course).isEmpty then
      DefaultSupportedRôles.foreach { case (name, (plus, minus)) =>
        rs.addSupportedRoleWithNegativeRights(course, name, plus.toArray, minus.toArray)
      }

  val DefaultSupportedRôles = Map[String, (Seq[Class[? <: Right]], Seq[Class[? <: Right]])](
    EnrollmentWebService.ROLE_STUDENT_NAME       -> (Seq(classOf[LearnCourseRight]) -> Seq.empty),
    EnrollmentWebService.ROLE_INSTRUCTOR_NAME    -> (Seq(classOf[TeachCourseRight]) -> Seq.empty),
    EnrollmentWebService.ROLE_ADVISOR_NAME       -> (Seq(
      classOf[ViewCourseGradeRight],
      classOf[ContentCourseRight],
      classOf[CourseRosterRight]
    ) -> Seq.empty),
    EnrollmentWebService.ROLE_TRIAL_LEARNER_NAME -> (Seq(classOf[LearnCourseRight]) -> Seq(classOf[FullContentRight])),
  )
end CoursePermissions
