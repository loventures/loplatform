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

package loi.cp.maintenanceWindow

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method, WebResponse}
import com.learningobjects.de.authorization.Secured
import loi.cp.overlord.OverlordRight

import scalaz.\/

/** Maintenance Window Crud Web Aoi.
  */
@Controller(value = "maintenanceWindows", root = true)
@RequestMapping(path = "maintenanceWindows")
@Secured(Array(classOf[OverlordRight]))
trait MaintenanceWindowRootApi extends ApiRootComponent:

  @RequestMapping(method = Method.GET)
  def get(apiQuery: ApiQuery): ApiQueryResults[MaintenanceWindow]

  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[MaintenanceWindow]

  /** Create a maintenance window according to the DTO and add a peer announcement that is associated with this
    * particular Maintenance Window.
    */
  @RequestMapping(method = Method.POST)
  def create(@RequestBody maintenanceWindow: MaintenanceWindowDTO): MaintenanceWindow

  @RequestMapping(path = "{id}", method = Method.PUT)
  def update(
    @PathVariable("id") id: Long,
    @RequestBody maintenanceWindow: MaintenanceWindowDTO
  ): ErrorResponse \/ MaintenanceWindow

  @RequestMapping(path = "{id}", method = Method.DELETE)
  def delete(@PathVariable("id") id: Long): ErrorResponse \/ WebResponse
end MaintenanceWindowRootApi
