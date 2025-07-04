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

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.HostingAdminRight
import loi.cp.right.RightBinding

import scalaz.\/

/** Announcement Crud Web Api.
  */
@Controller(value = "announcements", root = true)
@Secured(Array(classOf[AnnouncementAdminRight]))
trait AnnouncementRootApi extends ApiRootComponent:

  @RequestMapping(path = "announcements", method = Method.GET)
  def get(apiQuery: ApiQuery): ApiQueryResults[Announcement]

  @RequestMapping(path = "announcements/{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[Announcement]

  @RequestMapping(path = "announcements", method = Method.POST)
  def create(@RequestBody announcement: AnnouncementDTO): Announcement

  @RequestMapping(path = "announcements/{id}", method = Method.PUT)
  def update(@PathVariable("id") id: Long, @RequestBody announcement: AnnouncementDTO): ErrorResponse \/ Announcement

  @RequestMapping(path = "announcements/{id}", method = Method.DELETE)
  def delete(@PathVariable("id") id: Long): ErrorResponse \/ WebResponse

  @RequestMapping(path = "contexts/{context}/announcements", method = Method.GET)
  def getInContext(@PathVariable("context") context: Long, apiQuery: ApiQuery): ApiQueryResults[Announcement]

  @RequestMapping(path = "contexts/{context}/announcements/{id}", method = Method.GET)
  def getInContext(@PathVariable("context") context: Long, @PathVariable("id") id: Long): Option[Announcement]

  @RequestMapping(path = "contexts/{context}/announcements", method = Method.POST)
  def createInContext(
    @PathVariable("context") context: Long,
    @RequestBody announcement: AnnouncementDTO
  ): ErrorResponse \/ Announcement

  @RequestMapping(path = "contexts/{context}/announcements/{id}", method = Method.PUT)
  def updateInContext(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
    @RequestBody announcement: AnnouncementDTO
  ): ErrorResponse \/ Announcement

  @RequestMapping(path = "contexts/{context}/announcements/{id}", method = Method.DELETE)
  def deleteInContext(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long
  ): ErrorResponse \/ WebResponse

  /** Query announcements that are active, have started, and haven't ended.
    */
  @RequestMapping(path = "announcements/active", method = Method.GET)
  @Secured(overrides = true)
  def getActive(apiQuery: ApiQuery): ApiQueryResults[Announcement]

  @RequestMapping(path = "announcements/hide", method = Method.POST)
  @Secured(overrides = true)
  def hide(@RequestBody annIdDTO: AnnouncementIdDTO): NoContentResponse
end AnnouncementRootApi

@RightBinding(name = "Announcement Administrator", description = "Managed system-wide announcements.")
abstract class AnnouncementAdminRight extends HostingAdminRight {}
