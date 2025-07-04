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

package loi.cp.admin_pages

import com.learningobjects.cpxp.component.annotation.{Controller, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight
import loi.cp.right.RightMatch

import scala.collection.Map
@Controller(value = "adminPages", root = true)
@RequestMapping(path = "adminPages")
@Secured(value = Array(classOf[AdminRight]), `match` = RightMatch.ANY)
trait AdminPagesApi extends ApiRootComponent:

  @RequestMapping(method = Method.GET)
  def getAdminPages: AdminPagesInfo

final case class AdminPagesInfo(
  adminPages: Map[String, List[AdminPage]]
)

final case class AdminPage(
  description: Option[String],
  icon: Option[String],
  name: Option[String],
  identifier: Option[String]
)
