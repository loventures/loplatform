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

package loi.cp.content

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.course.CourseSection
import loi.cp.course.lightweight.Lwc
import loi.cp.customisation.Customisation
import loi.cp.reference.EdgePath

import java.util.UUID
import scala.util.Try

/** A service to return the content from a course.
  *
  * This service provides both a view as a [[scalaz.Tree]] of [[CourseContent]] with a root corresponding to the course
  * asset itself, and as an ordered sequence of all playable [[CourseContent]] within the course.
  */
@Service
trait CourseContentService:

  /** Find all content within a given course, arranged as trees under the root node. This method applies the current
    * [[Customisation]] for the course to the contents.
    *
    * @param course
    *   the course for which to fetch content
    * @param rights
    *   caller's course rights
    * @return
    *   all content belonging to the specified course
    */
  def getCourseContents(course: Lwc, rights: Option[CourseRights] = None): Try[CourseContents]

  /** Get the tree branch from the course asset down to the requested piece of content.
    *
    * Returns a failure if there's a surprising problem, or successful none if the content is not found.
    */
  def getCourseContent(course: Lwc, path: EdgePath, rights: Option[CourseRights] = None): Try[Option[ContentPath]]

  /** Primarily internal-facing API to get course content where the caller can provide explicit customisation to
    * override the stored settings.
    *
    * @param course
    *   the course for which to fetch content
    * @param customisation
    *   the customization to apply
    * @return
    */
  def getCourseContentsInternal(course: Lwc, customisation: Customisation): Try[CourseContents]

  /** Finds the competencies that cannot be assessed because the teaching content has been hidden. We scan some of the
    * descendants of the hidden teaching content to find unassessable competencies but not all descendants. This is just
    * for simplicity. ?? What ??
    */
  def findUnassessables(ws: AttachedReadWorkspace, section: CourseSection): Set[UUID]
end CourseContentService
