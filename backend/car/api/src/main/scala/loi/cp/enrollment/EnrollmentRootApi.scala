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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.CourseAdminRight
import loi.cp.course.right.ManageCoursesReadRight
import loi.cp.enrollment.EnrollmentRootApi.{
  EnrollmentBatchDTO,
  EnrollmentDTO,
  EnrollmentTransitionDTO,
  UserEnrollmentDTO
}
import scalaz.\/

import java.lang as jl
import java.util.Date

@Component
@Controller(value = "enrollments", root = true, category = Controller.Category.CONTEXTS)
trait EnrollmentRootApi extends ApiRootComponent:

  @RequestMapping(path = "courses/{courseId}/enrollments", method = Method.GET)
  @Secured(value = Array(classOf[CourseAdminRight], classOf[ManageCoursesReadRight]))
  def getEnrollmentsForCourse(
    @PathVariable(value = "courseId") courseId: Long,
    query: ApiQuery
  ): ErrorResponse \/ ApiQueryResults[EnrollmentComponent]

  @RequestMapping(path = "courses/{courseId}/enrollments/byUser/{userId}", method = Method.GET)
  @Secured(value = Array(classOf[CourseAdminRight], classOf[ManageCoursesReadRight]))
  def getEnrollmentForCourseUser(
    @PathVariable(value = "courseId") courseId: Long,
    @PathVariable(value = "userId") userId: Long,
    query: ApiQuery
  ): ErrorResponse \/ ApiQueryResults[EnrollmentComponent]

  @RequestMapping(path = "courses/{courseId}/enrollments/batch", method = Method.POST)
  @Secured(value = Array(classOf[CourseAdminRight]))
  def createEnrollments(
    @PathVariable("courseId") courseId: Long,
    @RequestBody enrollmentDTO: EnrollmentBatchDTO
  ): ErrorResponse \/ List[EnrollmentComponent]

  @RequestMapping(path = "courses/{courseId}/enrollments/byUser/{userId}", method = Method.PUT)
  @Secured(value = Array(classOf[CourseAdminRight]))
  def updateUserEnrollment(
    @PathVariable("courseId") courseId: Long,
    @PathVariable("userId") userId: Long,
    @RequestBody enrollmentDTO: EnrollmentDTO
  ): ErrorResponse \/ EnrollmentComponent

  @RequestMapping(path = "courses/{courseId}/enrollments/{enrollmentId}", method = Method.PUT)
  @Secured(value = Array(classOf[CourseAdminRight]))
  def updateEnrollment(
    @PathVariable("courseId") courseId: Long,
    @PathVariable("enrollmentId") enrollmentId: Long,
    @RequestBody enrollmentDTO: UserEnrollmentDTO
  ): ErrorResponse \/ EnrollmentComponent

  @RequestMapping(path = "courses/{courseId}/enrollments/byUser/{userId}/transition", method = Method.POST)
  @Secured(value = Array(classOf[CourseAdminRight]))
  def transition(
    @PathVariable(value = "courseId") courseId: Long,
    @PathVariable(value = "userId") userId: Long,
    @RequestBody enrollmentTransitionDTO: EnrollmentTransitionDTO
  ): ErrorResponse \/ List[EnrollmentComponent]

  @RequestMapping(path = "courses/{courseId}/enrollments/byUser/{userId}", method = Method.DELETE)
  @Secured(value = Array(classOf[CourseAdminRight]))
  def deleteUserEnrollments(
    @PathVariable("courseId") courseId: Long,
    @PathVariable("userId") userId: Long
  ): ErrorResponse \/ NoContentResponse

  @RequestMapping(path = "courses/{courseId}/enrollments/{enrollmentId}", method = Method.DELETE)
  @Secured(value = Array(classOf[CourseAdminRight]))
  def deleteEnrollment(
    @PathVariable("courseId") courseId: Long,
    @PathVariable("enrollmentId") enrollmentId: Long
  ): ErrorResponse \/ NoContentResponse

  @RequestMapping(path = "courses/{courseId}/enrollments/byUser/{userId}", method = Method.POST)
  @Secured(value = Array(classOf[CourseAdminRight]))
  def addUserEnrollment(
    @PathVariable(value = "courseId") courseId: Long,
    @PathVariable(value = "userId") userId: Long,
    @RequestBody userEnrollmentDTO: UserEnrollmentDTO
  ): ErrorResponse \/ EnrollmentComponent
end EnrollmentRootApi

object EnrollmentRootApi:
  case class EnrollmentTransitionDTO(disabled: Boolean)
  case class EnrollmentDTO(roleId: Long)
  case class EnrollmentBatchDTO(ids: List[jl.Long], roleId: Long)
  case class UserEnrollmentDTO(roleId: Long, startTime: Date, stopTime: Date, disabled: Boolean)
