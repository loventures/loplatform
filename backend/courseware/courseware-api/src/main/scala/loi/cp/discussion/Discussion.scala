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

package loi.cp.discussion

import loi.cp.content.CourseContent
import loi.cp.course.CourseSection
import loi.cp.reference.ContentIdentifier

/** Service object for discussion boards.
  */
case class Discussion(courseContent: CourseContent, section: CourseSection, closed: Boolean):

  lazy val identifier = ContentIdentifier(section, courseContent.edgePath)
  lazy val discussion = DiscussionContent(courseContent.asset).get

  lazy val title: String = discussion.title

  lazy val gradeable: Boolean = discussion match
    case Discussion1Content(asset) => asset.data.gradable
end Discussion
