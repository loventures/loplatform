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

import com.learningobjects.cpxp.component.annotation.{
  Component,
  Controller,
  DeIgnore,
  PathVariable,
  QueryParam,
  RequestBody,
  RequestMapping
}
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.securedByImplementation
import loi.cp.content.ContentAccessService
import loi.cp.course.preview.{PreviewRole, PreviewService}
import loi.cp.user.UserComponent
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import scala.util.Try

@Component
@Controller(root = true)
@securedByImplementation
class InstructorStudoWebController(val componentInstance: ComponentInstance)(implicit
  cas: ContentAccessService,
  previewService: PreviewService,
  user: UserDTO,
  now: TimeSource
) extends ApiRootComponent
    with ComponentImplementation:
  @DeIgnore
  protected def this() = this(null)(using null, null, null, null)

  @RequestMapping(path = "lwc/{context}/studo", method = Method.GET)
  def getStudont(@PathVariable("context") context: Long, @QueryParam("role") role: String): Try[Option[UserComponent]] =
    for
      course      <- cas.getCourseAsInstructor(context, user)
      previewRole <- validateRole(course, role)
    yield previewService.findPreviewer(course, previewRole)

  @RequestMapping(path = "lwc/{context}/studo", method = Method.POST)
  def studo(@PathVariable("context") context: Long, @RequestBody studo: Studo): Try[UserComponent] =
    for
      course      <- cas.getCourseAsInstructor(context, user)
      previewRole <- validateRole(course, studo.role)
    yield previewService.getOrCreatePreviewer(course, previewRole)

  // Any instructor can studo as a learner. Only authors in preview sections can studo as an instructor.
  // Your authorhood is implicit in your instructor access to the preview section.
  private def validateRole(course: CourseComponent, role: String): Try[PreviewRole] =
    for
      previewRole <- PreviewRole.withNameOption(role) <@~* new InvalidRequestException(s"Role $role")
      validRole    = previewRole == PreviewRole.Learner || course.getGroupType == GroupType.PreviewSection
      _           <- validRole <@~* new InvalidRequestException(s"Instructor outside preview section")
    yield previewRole
end InstructorStudoWebController

final case class Studo(role: String)
