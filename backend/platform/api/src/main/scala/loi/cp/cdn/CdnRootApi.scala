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

package loi.cp.cdn

import com.learningobjects.cpxp.component.annotation.{Controller, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.overlord.OverlordRight

/** CDN utils API
  */
@Controller(value = "cdn", root = true)
@RequestMapping(path = "cdn")
@Secured(Array(classOf[OverlordRight]))
trait CdnRootApi extends ApiRootComponent:

  @RequestMapping(method = Method.GET)
  def get: Int

  @RequestMapping(method = Method.POST)
  def refresh: Unit
end CdnRootApi
