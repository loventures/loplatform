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

package loi.cp.redirect

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method, WebResponse}
import com.learningobjects.de.authorization.Secured
import scalaz.\/

@Controller(value = "redirects", root = true)
@RequestMapping(path = "redirects")
@Secured(Array(classOf[RedirectAdminRight]))
trait RedirectRootApi extends ApiRootComponent:

  @RequestMapping(method = Method.GET)
  def get(apiQuery: ApiQuery): ApiQueryResults[Redirect]

  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[Redirect]

  @RequestMapping(method = Method.POST)
  def create(@RequestBody redirect: Redirect): ErrorResponse \/ Redirect

  @RequestMapping(path = "{id}", method = Method.DELETE)
  def delete(@PathVariable("id") id: Long): WebResponse

  @RequestMapping(path = "{id}", method = Method.PUT)
  def update(@PathVariable("id") id: Long, @RequestBody redirect: Redirect): ErrorResponse \/ Redirect
end RedirectRootApi
