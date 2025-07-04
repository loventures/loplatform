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

package loi.cp.email

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method, WebResponse}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

/** Email web API. This primarily exists to serve debugging email connectivity.
  */
@Controller(value = "emails", root = true)
@RequestMapping(path = "emails")
@Secured(Array(classOf[AdminRight]))
trait EmailRootApi extends ApiRootComponent:

  /** Gets an email from the system.
    * @param id
    *   the email id
    * @return
    *   the email
    */
  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[Email]

  /** Gets emails from the system.
    * @param q
    *   the request query
    * @return
    *   the resulting emails
    */
  @RequestMapping(method = Method.GET)
  def get(q: ApiQuery): ApiQueryResults[Email]

  /** Sends a test email to the specified address.
    * @param address
    *   the target email address
    * @return
    *   OK
    */
  @RequestMapping(path = "test", method = Method.POST)
  def test(@QueryParam("address") address: String): WebResponse
end EmailRootApi
