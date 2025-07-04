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

package loi.cp.role

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method, WebResponse}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.user.SudoAdminRight
import loi.cp.admin.right.{AdminRight, CourseAdminRight}
import loi.cp.course.right.ManageCoursesReadRight
import loi.cp.role.DomainRolesRootApi.{AddRoleByContextDTO, RoleDTO}
import scalaz.\/

@Controller(value = "roles", root = true)
trait DomainRolesRootApi extends ApiRootComponent:

  /** Get the supported roles in the domain
    *
    * @return
    *   the supported roles in the domain
    */
  @RequestMapping(path = "roles", method = Method.GET)
  def getRoles: Seq[SupportedRole]

  @Secured(Array(classOf[CourseAdminRight], classOf[ManageCoursesReadRight]))
  @RequestMapping(path = "roles/byContext/{courseId}", method = Method.GET)
  def getRolesByContext(@PathVariable("courseId") id: Long): ErrorResponse \/ Seq[SupportedRole]

  /** Update a Role.
    */
  @Secured(Array(classOf[SudoAdminRight]))
  @RequestMapping(path = "roles/{id}", method = Method.PUT)
  def update(@PathVariable("id") id: Long, @RequestBody role: RoleDTO): ErrorResponse \/ RoleDTO

  /** Create a new Role.
    */
  @Secured(Array(classOf[SudoAdminRight]))
  @RequestMapping(path = "roles", method = Method.POST)
  def create(@RequestBody role: RoleDTO): ErrorResponse \/ RoleDTO

  /** Add a Role.
    */
  @Secured(Array(classOf[SudoAdminRight]))
  @RequestMapping(path = "roles/byContext/{contextId}", method = Method.POST)
  def addRoleByContext(
    @PathVariable("contextId") contextId: Long,
    @RequestBody dto: AddRoleByContextDTO
  ): ErrorResponse \/ WebResponse

  /** Get the role types that are possible for a group - from this list a Supported Role can be created.
    *
    * @return
    *   the role types of the domain
    */
  @RequestMapping(path = "roleTypes", method = Method.GET)
  def getRoleTypes: Seq[RoleType]

  /** Delete a Role.
    */
  @Secured(Array(classOf[SudoAdminRight]))
  @RequestMapping(path = "roles/{id}", method = Method.DELETE)
  def delete(@PathVariable("id") id: Long): Unit

  /** Delete a Role for really realz.
    */
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "roles/deleteForReal/{id}", method = Method.DELETE)
  def deleteForReal(@PathVariable("id") id: Long): Unit
end DomainRolesRootApi

object DomainRolesRootApi:
  case class RoleDTO(name: String, roleId: String, addingSupported: Boolean, supportedRole: String, id: Long)
  case class AddRoleByContextDTO(roleId: Long)
