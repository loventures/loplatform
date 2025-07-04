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
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.customisation.Customisation

/** A service for materializing [[CourseSection]] s. This service computes view of the course with content and metadata.
  */
@Service
trait CourseSectionService:

  /** Returns a view of the course and its content, irrespective of the user, with the current customization for the
    * course. This will return generalized availability and due dates.
    *
    * @param sectionId
    *   the id of the section to fetch
    * @return
    *   the course with content and generalized availability and due dates and default customization
    */
  def getCourseSection(sectionId: Long, rights: Option[CourseRights] = None): Option[CourseSection]

  /** Returns a view of the course and its content irrespective of the user with a given customization. This will return
    * generalized availability and due dates. This is intended to get raw views of the content, particularly for course
    * customization controllers.
    *
    * @param sectionId
    *   the id of the section to fetch
    * @param customisation
    *   the customization to apply
    * @return
    *   the course with content and generalized availability and due dates and specified customization
    */
  def getCourseSectionInternal(sectionId: Long, customisation: Customisation): Option[CourseSection]
end CourseSectionService
