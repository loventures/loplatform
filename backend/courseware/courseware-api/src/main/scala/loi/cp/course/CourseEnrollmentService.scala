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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO

/** Some methods that were on [[CourseComponent]] and now are here.
  *
  * You could do all of this with `EnrollmentWebService`, but this is easier.
  *
  * The names should be self-explanatory.
  */
@Service
trait CourseEnrollmentService:

  /** Get students that are actively enrolled in the specified course.
    *
    * @param context
    *   The course ID.
    * @return
    *   All active students in the course
    */
  def getEnrolledStudentDTOs(context: Long): List[UserDTO]

  def getEnrolledStudentIds(context: Long): List[Long]

  def areAllStudentsEnrolled(context: Long, ids: Set[Long]): Boolean

  /** Get instructors that are actively enrolled in the specified course.
    *
    * @param context
    *   The course ID.
    * @return
    *   All active instructors in the course
    */
  def getEnrolledInstructorDTOs(context: Long): List[UserDTO]
end CourseEnrollmentService
