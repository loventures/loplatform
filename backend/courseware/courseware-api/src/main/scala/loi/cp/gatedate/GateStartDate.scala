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

package loi.cp.gatedate
import java.time.{Instant, ZoneId, ZonedDateTime}

import com.learningobjects.cpxp.service.enrollment.EnrollmentFacade
import loi.cp.course.lightweight.Lwc

// TODO: Clearly define the relationship for this with the start date
/** Algebra for the start date for computing gate dates. */
sealed trait GateStartDate:

  /** The start date, if applicable. */
  def value: Option[ZonedDateTime]

/** An instructor in a rolling-enrollment course. */
case object InstructorWithoutStartDate extends GateStartDate:
  override def value: Option[ZonedDateTime] = None

/** A user-centric start date. */
final case class StudentStartDate(start: ZonedDateTime) extends GateStartDate:
  override def value: Option[ZonedDateTime] = Some(start)

/** A course-centric start date. */
final case class CourseStartDate(start: ZonedDateTime) extends GateStartDate:
  override def value: Option[ZonedDateTime] = Some(start)

/** Utility for determining at what point the course is available when considering availability dates (gating).
  */
object GateStartDate:

  def forCourse(
    selfStudy: Boolean,
    startDate: Option[Instant],
    createDate: Instant,
    timeZone: ZoneId
  ): Option[GateStartDate] =
    if !selfStudy then Some(CourseStartDate(startDate.getOrElse(createDate).atZone(timeZone)))
    else None

  /** Get the effective start date of a course, or None in a self-study course. The start date is rewound to 00:00 at
    * the start of the day for the purposes of computing gate dates.
    */
  def forUser(
    course: Lwc,
    enrollments: List[EnrollmentFacade],
    instructorLike: Boolean,
    timeZone: ZoneId
  ): Option[GateStartDate] =
    if !course.rollingEnrollment then
      Some(CourseStartDate(course.startDate.getOrElse(course.createDate).atZone(timeZone)))
    else if instructorLike then Some(InstructorWithoutStartDate)
    else
      course
        .startDateForUser(enrollments)
        .map(_.atZone(timeZone))
        .map(StudentStartDate.apply)
end GateStartDate
