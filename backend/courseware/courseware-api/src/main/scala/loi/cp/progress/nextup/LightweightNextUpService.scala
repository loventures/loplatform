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

package loi.cp.progress
package nextup

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.content.CourseContent
import loi.cp.course.CourseSection
import loi.cp.lwgrade.StudentGradebook

@Service
trait LightweightNextUpService:

  /** Look up the next-up piece of content for the given user in the given lightweight course.
    *
    * The next-up content is the first piece of content, in the in-order traversal order, which has no progress at all.
    *
    * @return
    *   The piece of content that is next up for the given user, or `None` if the user has completed the entire course.
    */
  def nextUpContent(section: CourseSection, user: UserDTO, gradebook: StudentGradebook): Option[CourseContent]
end LightweightNextUpService
