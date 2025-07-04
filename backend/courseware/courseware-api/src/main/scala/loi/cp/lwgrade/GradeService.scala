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

package loi.cp.lwgrade

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.content.{CourseContent, CourseContents}
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.lwgrade.error.ColumnMissing
import loi.cp.offering.PublishAnalysis
import scalaz.syntax.std.list.*
import scalaz.{NonEmptyList, \/}

import java.time.Instant

@Service
trait GradeService:

  /** Return all the student gradebooks for a set of users. Some filled StudentGradebook will always be returned even if
    * the user is not a student in that course.
    *
    * @param course
    *   the course
    * @param contents
    *   the course contents for `course`
    * @param students
    *   the users
    */
  def getCourseGradebooks(
    course: ContextId,
    contents: CourseContents,
    students: NonEmptyList[UserId]
  ): Map[UserId, StudentGradebook]

  final def getGradebooks(section: CourseSection, users: List[UserId]): Map[UserId, StudentGradebook] =
    users.toNel.cata(users => getCourseGradebooks(section, section.contents, users), Map.empty)

  /** Returns all the grades for a user for a specific set of content if such a grade exists.
    */
  def getGrades(
    user: UserId,
    course: ContextId,
    contents: CourseContents
  ): StudentGradebook

  final def getGradebook(section: CourseSection, user: UserId): StudentGradebook =
    getGrades(user, section, section.contents)

  def deleteGradebook(section: CourseSection, user: UserId): Unit

  /** Moves the grades for `userId` from `srcCourse` to `tgtCourse`
    */
  def transferGrades(
    user: UserId,
    srcCourse: ContextId,
    tgtCourse: ContextId
  ): Unit

  /** Sets a specific grade for a user on an assignment, leverages the current structure that the grade service is aware
    * of for the course to calculate what the points awarded should be.
    *
    * @param user
    *   who is the grade getting set for
    * @param edgePath
    *   which item in the course are we setting a grade for
    * @param percent
    *   percent value the user has been awarded, a value between 0 and 1. This will be immediately scaled to the points
    *   possible on this gradeable thing.
    * @param when
    *   when should the grade set be attributed
    */
  def setGradePercent(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent,
    percent: Double,
    when: Instant,
  ): ColumnMissing \/ Grade

  def setGradePercent(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent,
    structure: GradeStructure,
    column: GradeColumn,
    percent: Double,
    when: Instant,
  ): Grade

  def setGradePending(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent,
    structure: GradeStructure,
    column: GradeColumn,
    when: Instant
  ): (Option[Grade], Grade)

  /** Unsets a grade. This effectively puts it in an ungraded state but should not affect history.
    */
  def unsetGrade(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent
  ): ColumnMissing \/ Grade

  def scheduleGradeUpdate(
    section: LightweightCourse,
    changes: PublishAnalysis.LineItemContainer
  ): Unit
end GradeService
