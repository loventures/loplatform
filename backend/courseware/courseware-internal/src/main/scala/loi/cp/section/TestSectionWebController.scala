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

package loi.cp.section

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.ErrorResponse.badRequest
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.course.model.Course
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.accesscode.CourseAccessCodeService
import loi.cp.content.ContentAccessService
import loi.cp.course.lightweight.LightweightCourseService
import loi.cp.course.{CourseConfigurationService, CoursePreferences}
import loi.cp.web.ErrorResponses.*
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scaloi.syntax.BooleanOps.*

@Component
@Controller(value = "lwc-course", root = true)
class TestSectionWebController(val componentInstance: ComponentInstance)(implicit
  contentAccessService: ContentAccessService,
  courseAccessCodeService: CourseAccessCodeService,
  courseConfigurationService: CourseConfigurationService,
  lwcService: LightweightCourseService,
  nodeService: AssetNodeService,
  user: UserDTO,
  workspaceService: ReadWorkspaceService,
) extends ApiRootComponent
    with ComponentImplementation:
  import TestSectionWebController.*

  @RequestMapping(path = "lwc/{context}/update", method = Method.GET)
  def updateAvailable(@PathVariable("context") context: Long): ErrorResponse \/ Boolean =
    for
      section <- contentAccessService.getCourseAsAdministrator(context, user).orErrorResponse
      branch   = section.loadBranch()
      _       <- branch.project \/> badRequest("missing branch project")
    yield section.commitId != branch.head.id

  @RequestMapping(path = "lwc/{context}/update", method = Method.POST)
  def update(@PathVariable("context") context: Long): ErrorResponse \/ Boolean =
    for
      section <- contentAccessService.getCourseAsAdministrator(context, user).orErrorResponse
      branch   = section.loadBranch()
      project <- branch.project \/> badRequest("missing branch project")
      ws      <- workspaceService.requireReadWorkspace(branch.id).right
      course  <- nodeService.loadA[Course](ws).byName(project.homeName).orErrorResponse
    yield

      courseConfigurationService.copyConfigOrThrow(CoursePreferences, project.id, section)

      (section.commitId != branch.head.id) <|? {
        section.setCommitId(branch.head.id)
        section.setCourseId(course.info.id)
        lwcService.incrementGeneration(section)
      }

  @RequestMapping(path = "lwc/{context}/accessCode", method = Method.GET)
  def getAccessCode(@PathVariable("context") context: Long): ErrorResponse \/ Option[String] =
    for section <- contentAccessService.getCourseAsAdministrator(context, user).orErrorResponse
    yield courseAccessCodeService.getAccessCode(section).map(_.getAccessCode)

  @RequestMapping(path = "lwc/{context}/accessCode", method = Method.POST)
  def generateAccessCode(@PathVariable("context") context: Long): ErrorResponse \/ String =
    for section <- contentAccessService.getCourseAsAdministrator(context, user).orErrorResponse
    yield courseAccessCodeService.getOrCreateAccessCode(section).getAccessCode
end TestSectionWebController

object TestSectionWebController:
  import org.log4s.*

  private implicit final val logger: Logger = getLogger
