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
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.course.{CourseSection, CourseSectionService, CourseWorkspaceService}
import loi.cp.discussion.{Discussion, DiscussionBoardService}
import loi.cp.reference.EdgePath
import scalaz.\/
import scalaz.syntax.std.option.*

/** Web utilities for lightweight courses
  */
@Service
class CourseWebUtils(
  courseSectionService: CourseSectionService,
  courseWorkspaceService: CourseWorkspaceService,
  discussionBoardService: DiscussionBoardService
):
  def loadCourseSection(courseId: Long, rights: Option[CourseRights] = None): String \/ CourseSection =
    courseSectionService.getCourseSection(courseId, rights).toRightDisjunction(s"No such course $courseId")

  def sectionOrThrow404(sectionId: Long): CourseSection =
    loadCourseSection(sectionId).valueOr(err => throw new ResourceNotFoundException(err))

  def sectionWsOrThrow404(sectionId: Long): (CourseSection, AttachedReadWorkspace) =
    val section = sectionOrThrow404(sectionId)
    val ws      = courseWorkspaceService.loadReadWorkspace(section)
    (section, ws)

  def discussionOrThrow404(section: CourseSection, edgePath: EdgePath): Discussion =
    val discussion = for
      content    <- section.contents
                      .get(edgePath)
                      .toRightDisjunction(s"No such content; sectionId: ${section.id}; edgePath: $edgePath")
      discussion <- discussionBoardService
                      .getDiscussion(section, content)
                      .toRightDisjunction(s"Not a discussion; sectionId: ${section.id}; edgePath: $edgePath")
    yield discussion

    discussion.valueOr(err => throw new ResourceNotFoundException(err))
  end discussionOrThrow404

  def loadCourseSectionContents(
    courseId: Long,
    edgePath: EdgePath,
    rights: Option[CourseRights] = None
  ): String \/ (CourseSection, CourseContent) =
    for
      section <- courseSectionService.getCourseSection(courseId, rights).toRightDisjunction(s"No such course $courseId")
      content <- section.contents.get(edgePath).toRightDisjunction(s"No such content $edgePath")
    yield (section, content)
end CourseWebUtils
