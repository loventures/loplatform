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

package loi.cp.overlord

import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.startup.StartupTaskScope.Overlord
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding}
import loi.cp.admin.right.{HostingAdminRight, UserAdminRight}
import loi.cp.role.{RoleService, SupportedRoleFacade}
import loi.cp.role.RoleService.JsonRole

import scala.jdk.CollectionConverters.*

/** Adds the overlord domain supported roles and rights.
  */
@StartupTaskBinding(version = 20171106, taskScope = Overlord)
class OverlordRoleStartupTask(rs: RoleService, domain: DomainDTO) extends StartupTask:
  import OverlordRoleStartupTask.*

  override def run(): Unit =
    if Option(rs.getRoleByRoleId(UnderlördeId)).isEmpty then
      val role = new JsonRole
      role.roleId = UnderlördeId
      role.name = "Underlörde"
      role.rights = List(classOf[UnderlordRight].getName, classOf[HostingAdminRight].getName).asJava
      rs.createRole(role)
    else rs.addRightsToRole(domain, UnderlördeId, classOf[UnderlordRight], classOf[HostingAdminRight])

    type AddRole = (String, List[Class[? <: loi.cp.right.Right]]) => Unit
    def addSupportedRoleAndRights(supportedRoles: List[SupportedRoleFacade]): AddRole = (roleId, roles) =>
      if supportedRoles.map(_.getRole.getRoleId).contains(roleId) then rs.addRightsToRole(domain, roleId, roles*)
      else rs.addSupportedRole(domain, "role-" + roleId, roles*)
    val supportedRoles                                                                = rs.getSupportedRoles.asScala.toList
    addSupportedRoleAndRights(supportedRoles)(OverlördeId, List(classOf[OverlordRight], classOf[HostingAdminRight]))
    addSupportedRoleAndRights(supportedRoles)(SuppörtId, List(classOf[SupportRight], classOf[UserAdminRight]))
    addSupportedRoleAndRights(supportedRoles)(UnderlördeId, List(classOf[UnderlordRight], classOf[HostingAdminRight]))
  end run
end OverlordRoleStartupTask

object OverlordRoleStartupTask:
  final val OverlördeId  = "overlord"
  final val UnderlördeId = "underlord"
  final val SuppörtId    = "support"
