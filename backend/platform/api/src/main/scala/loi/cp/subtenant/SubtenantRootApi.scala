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

package loi.cp.subtenant

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method, WebResponse}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.{AdminRight, SubtenantAdminRight}
import loi.cp.attachment.AttachmentComponent
import loi.cp.right.RightMatch

import scalaz.\/

@Controller(value = "subtenants", root = true)
@RequestMapping(path = "subtenants")
@Secured(Array(classOf[SubtenantAdminRight]))
trait SubtenantRootApi extends ApiRootComponent:

  /** Query all the subtenants. */
  @RequestMapping(method = Method.GET)
  def get(apiQuery: ApiQuery): ApiQueryResults[Subtenant]

  /** Get a particular subtenant. */
  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[Subtenant]

  /** Query all the subtenants. */
  @RequestMapping(path = "names", method = Method.GET)
  @Secured(value = Array(classOf[AdminRight]), overrides = true, `match` = RightMatch.ANY)
  def getNames(apiQuery: ApiQuery): ApiQueryResults[SubtenantName]

  /** Get a particular subtenant. */
  @RequestMapping(path = "names/{id}", method = Method.GET)
  @Secured(value = Array(classOf[AdminRight]), overrides = true, `match` = RightMatch.ANY)
  def getName(@PathVariable("id") id: Long): Option[SubtenantName]

  /** Get a particular subtenant. */
  @RequestMapping(path = "{id}/icon", method = Method.GET)
  @Secured(value = Array(), overrides = true)
  def getLogo(@PathVariable("id") id: Long): Option[AttachmentComponent]

  /** Create a new subtenant. */
  @RequestMapping(method = Method.POST)
  def create(@RequestBody subtenant: Subtenant): ErrorResponse \/ Subtenant

  /** Delete a subtenant. */
  @RequestMapping(path = "{id}", method = Method.DELETE)
  def delete(@PathVariable("id") id: Long): WebResponse

  /** Get a particular subtenant. */
  @RequestMapping(path = "{id}", method = Method.PUT)
  def update(@PathVariable("id") id: Long, @RequestBody subtenant: Subtenant): ErrorResponse \/ Subtenant
end SubtenantRootApi
