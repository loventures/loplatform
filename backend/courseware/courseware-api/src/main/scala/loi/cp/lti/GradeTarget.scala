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

package loi.cp.lti

import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.content.CourseContent
import loi.cp.course.CourseSection
import loi.cp.reference.EdgePath

/** A Tool Provider wants to set a grade for these coordinates. We are the Tool Consumer */
case class GradeTarget(student: UserDTO, section: CourseSection, content: CourseContent):
  val studentId: Long    = student.id
  val contextId: Long    = section.id
  val edgePath: EdgePath = content.edgePath
  override val toString  = s"$studentId:$contextId:$edgePath"
