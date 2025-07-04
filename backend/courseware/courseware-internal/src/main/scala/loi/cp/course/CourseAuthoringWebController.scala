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

package loi.cp
package course

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method, RedirectResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.project.Project
import loi.authoring.security.right.ViewAllProjectsRight
import loi.cp.content.{ContentAccessService, CourseContentService}
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.reference.EdgePath
import loi.cp.right.RightService
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.syntax.boolean.*
import scaloi.syntax.ʈry.*

@Component
@Controller(root = true, value = "courseAuthoring")
@RequestMapping(path = "courseAuthoring")
class CourseAuthoringWebController(val componentInstance: ComponentInstance)(
  sas: CourseAccessService,
  cas: ContentAccessService,
  ccs: CourseContentService,
  rightService: RightService,
  user: => UserDTO
) extends ApiRootComponent
    with ComponentImplementation:
  @RequestMapping(path = "section/{id}", method = Method.GET)
  def toCourse(
    @PathVariable("id") id: Long,
  ): ErrorResponse \/ RedirectResponse =
    for course <- loadCourse(id)
    yield
      val asset = course.loadCourse()
      RedirectResponse.temporary(s"/Authoring/branch/${course.branch.id}/launch/${asset.info.name}")

  @RequestMapping(path = "section/{id}/content/{path}", method = Method.GET)
  def toCourseContent(
    @PathVariable("id") id: Long,
    @PathVariable("path") path: EdgePath,
  ): ErrorResponse \/ RedirectResponse =
    for
      course  <- loadCourse(id)
      rights  <- sas.actualRights(course, user) \/> ErrorResponse.badRequest("No course access")
      pathOpt <- ccs.getCourseContent(course, path, rights.some) \/>| ErrorResponse.badRequest(
                   "Unknown content error"
                 )
      path    <- pathOpt \/> ErrorResponse.badRequest("Missing content error")
      branch   = path.branch.flatten.map(_.asset)
      context  = branch.dropRight(1).map(_.info.name).mkString(".")
    yield RedirectResponse.temporary(
      s"/Authoring/branch/${course.branch.id}/launch/${branch.last.info.name}?contextPath=$context"
    )

  private def loadCourse(id: Long): ErrorResponse \/ LightweightCourse =
    for
      course  <- cas.getCourseAsInstructor(id, user) \/>| ErrorResponse.forbidden("No course access")
      project <- course.branch.project \/> ErrorResponse.badRequest("Missing project")
      _       <- canAccessProject(project) \/> ErrorResponse.forbidden("No project access")
    yield course

  private def canAccessProject(project: Project): Boolean =
    project.userIds.contains(user.id) || rightService.getUserHasRight(classOf[ViewAllProjectsRight])
end CourseAuthoringWebController
