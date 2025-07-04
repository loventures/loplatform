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

package loi.cp.announcement

import com.learningobjects.cpxp.component.annotation.{Component, PathVariable}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ErrorResponse, NoContentResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.course.lightweight.LightweightCourse
import scalaz.\/
import scalaz.syntax.std.option.*

/** Implementation of the Announcement Crud Web Api.
  */
@Component
class AnnouncementRootApiImpl(val componentInstance: ComponentInstance)(implicit
  domain: DomainDTO,
  user: UserDTO,
  cs: ComponentService,
  as: AnnouncementService
) extends AnnouncementRootApi
    with ComponentImplementation:
  override def get(apiQuery: ApiQuery): ApiQueryResults[Announcement] = as.get(domain, apiQuery)

  override def get(id: Long): Option[Announcement] = as.get(domain, id)

  override def create(announcement: AnnouncementDTO): Announcement =
    as.create(domain, announcement)

  override def update(id: Long, announcement: AnnouncementDTO): ErrorResponse \/ Announcement =
    for oldAnnouncement <- get(id) \/> ErrorResponse.notFound
    yield as.update(oldAnnouncement, announcement)

  override def delete(id: Long): ErrorResponse \/ WebResponse =
    for announcement <- get(id) \/> ErrorResponse.notFound
    yield
      as.delete(announcement)
      NoContentResponse

  override def getInContext(context: Long, apiQuery: ApiQuery): ApiQueryResults[Announcement] =
    context.component_?[LightweightCourse].cata(as.get(_, apiQuery), ApiQueryResults.emptyResults[Announcement]);

  override def getInContext(context: Long, id: Long): Option[Announcement] =
    context.component_?[LightweightCourse].flatMap(context => as.get(context, id))

  override def createInContext(
    @PathVariable("context") context: Long,
    announcement: AnnouncementDTO
  ): ErrorResponse \/ Announcement =
    for course <- context.component_?[LightweightCourse] \/> ErrorResponse.notFound
    yield as.create(course, announcement)

  override def updateInContext(
    @PathVariable("context") context: Long,
    id: Long,
    announcement: AnnouncementDTO
  ): ErrorResponse \/ Announcement =
    for oldAnnouncement <- getInContext(context, id) \/> ErrorResponse.notFound
    yield as.update(oldAnnouncement, announcement)

  override def deleteInContext(@PathVariable("context") context: Long, id: Long): ErrorResponse \/ WebResponse =
    for announcement <- getInContext(context, id) \/> ErrorResponse.notFound
    yield
      as.delete(announcement)
      NoContentResponse

  override def getActive(apiQuery: ApiQuery): ApiQueryResults[Announcement] = as.getActive(apiQuery)

  override def hide(annIdDTO: AnnouncementIdDTO): NoContentResponse =
    as.hide(annIdDTO.announcementId)
    NoContentResponse
end AnnouncementRootApiImpl
