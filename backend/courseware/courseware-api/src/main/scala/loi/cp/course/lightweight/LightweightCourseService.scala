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
package lightweight

import com.learningobjects.cpxp.component.annotation.Service

@Service
trait LightweightCourseService:

  /** Initialise a newly-created [[LightweightCourse]].
    *
    * If `origin` is provided, certain pertinent data are copied from it onto the new `course`.
    */
  def initializeSection(course: LightweightCourse, origin: Option[LightweightCourse]): Unit

  def updateSection(course: LightweightCourse): Unit

  /** Indicate that the provided course has aged in a way that potentially alters a user's experience thereof.
    */
  def incrementGeneration(lwc: Lwc): Unit
end LightweightCourseService
