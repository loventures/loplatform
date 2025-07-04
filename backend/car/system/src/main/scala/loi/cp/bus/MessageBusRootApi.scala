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

package loi.cp.bus

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.IntegrationAdminRight

@Controller(value = "messageBuses", root = true)
@RequestMapping(path = "messageBuses")
@Secured(Array(classOf[IntegrationAdminRight]))
trait MessageBusRootApi extends ApiRootComponent:
  @RequestMapping(method = Method.GET)
  def queryMessageBuses(apiQuery: ApiQuery): ApiQueryResults[MessageBus]

  @RequestMapping(path = "{id}", method = Method.GET)
  def getMessageBus(@PathVariable("id") id: Long): Option[MessageBus]

  @RequestMapping(path = "{id}/pause", method = Method.PUT)
  def pauseMessageBus(@PathVariable("id") id: Long): Unit

  @RequestMapping(path = "{id}/resume", method = Method.PUT)
  def resumeMessageBus(@PathVariable("id") id: Long): Unit

  @RequestMapping(path = "{id}/stop", method = Method.PUT)
  def stopMessageBus(@PathVariable("id") id: Long): Unit
end MessageBusRootApi
