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

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.relationship.{RelationshipConstants, RelationshipWebService, RoleFacade}
import loi.cp.context.ContextComponent
import loi.cp.role.DomainRolesRootApi.RoleDTO
import loi.cp.role.impl.RoleParentFacade

import scala.jdk.CollectionConverters.*

@Service
class DomainRoleService(implicit
  fs: FacadeService,
  srs: SupportedRoleService,
  rws: RelationshipWebService,
  rs: RoleService,
  cs: ComponentService
):

  def parent: RoleParentFacade =
    RelationshipConstants.ID_FOLDER_ROLES.facade[RoleParentFacade]

  def getRoles(contextId: Option[java.lang.Long]): Seq[SupportedRole] =
    val HostingAdminRoles = rs.getHostingAdminRoleNames
    val context           = contextId.flatMap(id => id.component_?[ContextComponent]).getOrElse(Current.getDomainDTO)
    srs
      .getAllRoles(context)
      .asScala
      .filter(role =>
        val id = role.roleType.id
        id == -1.toLong || id.facade_?[RoleFacade].exists(role => !HostingAdminRoles.contains(role.getIdStr))
      )
      .toIndexedSeq
  end getRoles

  def addSupported(role: RoleFacade): Long =
    if !rs.getDomainRoles.contains(role) then
      val supported = rs.addSupportedRole
      supported.setRole(role)
      supported.setRights(new SupportedRoleFacade.RightsList)
      supported.getId
    else
      val idx = rs.getDomainRoles.indexOf(role)
      rs.getDomainRoles.get(idx).getId

  def addNewRole(newRole: RoleDTO): Long =
    val folderId = rws.getRoleFolder
    val role     = rws.addRole(folderId)
    role.setRoleId(newRole.roleId)
    role.setName(newRole.name)
    addSupported(role)

  def update(rf: RoleFacade, role: RoleDTO): Unit =
    rf.setRoleId(role.roleId)
    rf.setName(role.name)

  def delete(id: Long): Unit = id.facade_?[SupportedRoleFacade].foreach(_.delete())

  def deleteForReal(id: Long): Unit = id.facade_?[RoleFacade].foreach(_.delete())
end DomainRoleService
