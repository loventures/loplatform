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

package loi.cp.right

import com.learningobjects.cpxp.component.annotation.{Controller, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.RoleAdminRight
import loi.cp.right.RightRootApi.{RightDesc, RoleRightMap}
import loi.cp.role.SupportedRole
import scalaz.\/

import java.lang as jl

@Controller(value = "rights", root = true)
@Secured(value = Array(classOf[RoleAdminRight]))
trait RightRootApi extends ApiRootComponent:

  @RequestMapping(path = "rights", method = Method.GET)
  def getRightsTree: RightDTO

  @RequestMapping(path = "rights/course", method = Method.GET)
  def getCourseRightTree: RightDTO

  @RequestMapping(path = "rights/all", method = Method.GET)
  def getAllRights: List[RightDesc]

  @RequestMapping(path = "rights/course/all", method = Method.GET)
  def getCourseRights: List[RightDesc]

  @RequestMapping(path = "rights", method = Method.POST)
  def updateRights(@RequestBody roleRightMap: RoleRightMap): ErrorResponse \/ Seq[SupportedRole]
end RightRootApi

object RightRootApi:
  case class RoleRightMap(rolesToRights: Map[String, List[String]], contextId: Option[jl.Long])

  case class RightDesc(name: String, description: String, clasz: Class[? <: Right])
