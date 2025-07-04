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

package loi.cp.user

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestMapping}
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured

/** A public API for accessing user profiles by their opaque handle.
  */
@Controller(value = "profiles", root = true)
@RequestMapping(path = "profiles")
@Secured // logged in but no other auth required
trait ProfileRootApi extends ApiRootComponent:

  /** Get a list of profiles. The only supported query is `prefilter=handle:in(h1,...)`
    */
  @RequestMapping(method = Method.GET)
  def getProfiles(query: ApiQuery): Seq[Profile]

  /** Get a profile by handle. */
  @RequestMapping(path = "{handle}", method = Method.GET)
  def getProfile(@PathVariable("handle") handle: String): Option[Profile]
end ProfileRootApi
