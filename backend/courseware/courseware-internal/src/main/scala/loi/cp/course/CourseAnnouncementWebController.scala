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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.securedByImplementation
import loi.cp.announcement.{Announcement, AnnouncementService}
import loi.cp.content.ContentAccessService
import scaloi.syntax.option.*

import scala.util.Try

@securedByImplementation
@Controller(root = true)
@Component
class CourseAnnouncementWebController(val componentInstance: ComponentInstance)(implicit
  contentAccessService: ContentAccessService,
  announcementService: AnnouncementService,
  user: UserDTO,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "lwc/{context}/announcements/active", method = Method.GET)
  def activeAnnouncements(
    @PathVariable("context") context: Long,
    apiQuery: ApiQuery,
  ): Try[ApiQueryResults[Announcement]] =
    for course <- contentAccessService.getCourseAsLearner(context, user)
    yield announcementService.getActive(apiQuery, course.getOfferingId ::? List(course.id))

  @RequestMapping(path = "lwc/{context}/announcements/{id}/hide", method = Method.POST)
  def hideAnnouncement(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
  ): Try[Unit] =
    for _ <- contentAccessService.getCourseAsLearner(context, user)
    yield announcementService.hide(id) // the announcement may be from anywhere... so just hide it
end CourseAnnouncementWebController
