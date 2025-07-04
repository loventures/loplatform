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
package overview

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType as EnrolmentType
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService as EnrolmentWebService
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.lwgrade.GradeService
import loi.cp.progress.LightweightProgressService
import loi.cp.progress.nextup.LightweightNextUpService
import loi.cp.progress.report.Progress
import loi.cp.reference.EdgePath
import loi.cp.role.RoleService
import scalaz.*
import scaloi.syntax.CollectionBoxOps.*

import java.util as ju
import scala.jdk.CollectionConverters.*

/** A service for computing course overview data from the sundry other parts of the application which know better.
  */
@Service
trait CourseOverviewService:

  // for the student view

  /** Fetch progress data for the provided student and courses, and return progress objects for all courses in which the
    * student has progress.
    *
    * @return
    *   a mapping from the ID of courses in which the student has earned progress to progress DTOs
    */
  def progresses(
    student: UserDTO,
    courses: List[CourseComponent],
  ): Long =?> EnrolledCourseOverview.Progress

  /** Fetch the next-up content item for the provided student and courses.
    *
    * @return
    *   a mapping from the ID of courses which have a next-up content item for the provided user
    */
  def nextUps(
    student: UserDTO,
    courses: List[CourseComponent],
  ): Long =?> NextUpSummary

  // for the instructor view

  /** Count the students in the provided courses.
    *
    * @return
    *   a mapping from the IDs of the provided courses to the number of students enrolled in that course
    */
  def studentCounts(courses: List[CourseComponent]): Long =?> Int

  // usefulness

  def studentRoleIds: List[Long]
  def instructorRoleIds: List[Long]
end CourseOverviewService

@Service
private class CourseOverviewServiceImpl(
  courseSectionService: CourseSectionService,
  enrolmentWebService: EnrolmentWebService,
  gradeService: GradeService,
  lwNextUpService: LightweightNextUpService,
  lwProgressService: LightweightProgressService,
  roleService: RoleService,
) extends CourseOverviewService:

  private def partitionCourses(
    cs: List[CourseComponent],
  ): List[CourseSection] =
    cs.map { case LightweightCourse(lwc) =>
      courseSectionService.getCourseSection(lwc.id).get
    }

  override def progresses(user: UserDTO, courses: List[CourseComponent]): Long =?> Progress = Kleisli {
    val lwCourses    = partitionCourses(courses)
    val lwProgresses = lwCourses.flatMap { lwc =>
      val gradebook    = gradeService.getGradebook(lwc, user)
      val progressbook = lwProgressService.loadProgress(lwc, user, gradebook)
      progressbook.get(EdgePath.Root).map(progress => lwc.id -> progress)
    }
    lwProgresses.toMap.lift
  }

  override def nextUps(user: UserDTO, courses: List[CourseComponent]): Long =?> NextUpSummary =
    val lwCourses = partitionCourses(courses)

    val lwNextUps = for
      section  <- lwCourses
      gradebook = gradeService.getGradebook(section, user)
      nextUp   <- lwNextUpService.nextUpContent(section, user, gradebook)
    yield section.id -> NextUpSummary.fromCourseContent(nextUp)

    Kleisli(lwNextUps.toMap.lift)
  end nextUps

  override def studentCounts(courses: List[CourseComponent]): Long =?> Int = Kleisli {
    enrolmentWebService
      .getGroupUserCounts(
        courses.map(_.getId).toSet.asJava,
        studentRoleIds.toSet[Long].boxInsideTo[ju.Set](),
        EnrolmentType.ACTIVE_ONLY,
      )
      .asScala
      .map { case (groupId, count) => (groupId: Long, count: Int) }
      .lift
  }

  override def instructorRoleIds: List[Long] =
    List("instructor", "advisor")
      .map(ri => roleService.getRoleByRoleId(ri).getId: Long)
  override def studentRoleIds: List[Long]    =
    List("student", "trialLearner")
      .map(ri => roleService.getRoleByRoleId(ri).getId: Long)
end CourseOverviewServiceImpl
