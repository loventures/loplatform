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

package loi.cp.lwgrade.update

import com.learningobjects.cpxp.service.presence.EventType
import loi.cp.lwgrade.Grade
import loi.cp.reference.EdgePath

/** A message object for when a user's grade in a context is updated.
  *
  * @param courseId
  *   the context where progress was made
  * @param userId
  *   the user the grade is for
  * @param edgePath
  *   which item's grade has been updated.
  * @param grade
  *   the updated grade
  */
case class GradeUpdate(courseId: Long, userId: Long, edgePath: EdgePath, grade: Grade)

object GradeUpdate:
  implicit val GradeUpdateType: EventType[GradeUpdate] = EventType("GradeUpdate")
