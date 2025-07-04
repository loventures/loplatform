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

import java.time.Instant

import argonaut.CodecJson
import scaloi.json.ArgoExtras.instantCodec

// For the shape of some of these, see CoursesAPI.js and CourseUtils.js

/** A POJO representing all the data you would need to present for a course overview in a course listing page.
  */
final case class EnrolledCourseOverview(
  course: EnrolledCourseOverview.Course,
  roleId: String,
  startTime: Option[Instant],
  stopTime: Option[Instant],
  overallProgress: Option[EnrolledCourseOverview.Progress],
  nextUp: Option[NextUpSummary],
  enrolledStudents: Option[Int],
  overallGrade: Option[Double],
)
object EnrolledCourseOverview:
  final case class Course(
    id: Long,
    courseName: String,
    courseGuid: String, // i.e., groupId
    projectName: String,
    url: String,
    startDate: Option[Instant],
    endDate: Option[Instant],
    shutdownDate: Option[Instant],
    configuredShutdownDate: Option[Instant]
  )
  object Course:
    implicit val codec: CodecJson[Course] =
      CodecJson.derive[Course]
  // now, there's a loi.cp.course.Progress, of course,
  // and import does not shadow def in other unit, yet
  type Progress = loi.cp.progress.report.Progress
  val Progress = loi.cp.progress.report.Progress

  implicit val codec: CodecJson[EnrolledCourseOverview] =
    CodecJson.derive[EnrolledCourseOverview]
end EnrolledCourseOverview
