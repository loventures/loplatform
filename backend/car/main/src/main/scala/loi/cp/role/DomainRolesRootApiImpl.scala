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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{ErrorResponse, NoContentResponse, WebResponse}
import com.learningobjects.cpxp.component.{AbstractComponent, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.relationship.{RelationshipWebService, RoleFacade}
import com.learningobjects.cpxp.util.InternationalizationUtils
import loi.cp.context.ContextComponent
import loi.cp.role.DomainRolesRootApi.{AddRoleByContextDTO, RoleDTO}
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.syntax.BooleanOps.*

import scala.jdk.CollectionConverters.*

@Component
class DomainRolesRootApiImpl(
  supportedRoleService: SupportedRoleService,
  roleService: RoleService,
  domainRoleService: DomainRoleService,
  rws: RelationshipWebService
)(implicit fs: FacadeService, cs: ComponentService)
    extends AbstractComponent
    with DomainRolesRootApi:

  override def getRoles: Seq[SupportedRole] =
    domainRoleService.getRoles(None)

  override def getRolesByContext(id: Long): ErrorResponse \/ Seq[SupportedRole] =
    for context <- id.component_?[ContextComponent] \/> ErrorResponse.notFound
    yield supportedRoleService.getAllRoles(context).asScala.toSeq

  override def create(newRole: RoleDTO): ErrorResponse \/ RoleDTO =
    if newRole.addingSupported then addSupported(newRole)
    else addNewRole(newRole)

  override def addRoleByContext(contextId: Long, dto: AddRoleByContextDTO): ErrorResponse \/ WebResponse =
    for contextComponent <- contextId.component_?[ContextComponent] \/> ErrorResponse.notFound
    yield
      val roleFacade     = dto.roleId.facade[RoleFacade]
      val roleType       = RoleType.apply(roleFacade)
      val supportedRoles =
        roleService
          .getSupportedRoles(contextComponent)
          .asScala
          .map(facade =>
            val rights = facade.getRights.asScala.toList
            new SupportedRole(facade.getId, RoleType.apply(facade.getRole), rights)
          )
      if !supportedRoles.exists(sr => sr.roleType.roleId == roleType.roleId) then
        roleService.addSupportedRole(contextComponent, roleFacade)
      NoContentResponse

  override def update(id: Long, role: RoleDTO): ErrorResponse \/ RoleDTO =
    for
      supportedRoleFacade <- Option(fs.getFacade(id, classOf[SupportedRoleFacade])) \/> ErrorResponse.notFound
      _                   <- domainRoleService.parent.lock(true) \/> ErrorResponse.serverError
      folderId             = rws.getRoleFolder
      roleFacade           = Option(rws.getRoleByRoleId(folderId, role.roleId))
      rf                   = supportedRoleFacade.getRole
      _                   <- roleFacade.forall(r => r.getId == rf.getId) \/> duplicateRoleIdError(role)
    yield
      domainRoleService.update(rf, role)
      role.copy(id = id)

  override def getRoleTypes: Seq[RoleType] =
    roleService.getKnownRoles.asScala
      .map(RoleType.apply)
      .toSeq
      .sorted

  override def delete(id: Long): Unit =
    domainRoleService.delete(id)

  override def deleteForReal(id: Long): Unit =
    domainRoleService.deleteForReal(id)

  private def addSupported(newRole: RoleDTO): ErrorResponse \/ RoleDTO =
    for roleFacade <- Option(fs.getFacade(newRole.supportedRole.toLong, classOf[RoleFacade])) \/> ErrorResponse.notFound
    yield
      val supportedRoleId = domainRoleService.addSupported(roleFacade)
      val name            =
        Option(roleFacade.getName).getOrElse(InternationalizationUtils.formatMessage(roleFacade.getMsg + ".name"))
      RoleDTO(name, roleFacade.getRoleId, newRole.addingSupported, newRole.supportedRole, supportedRoleId)

  private def addNewRole(newRole: RoleDTO): ErrorResponse \/ RoleDTO =
    for
      _ <- domainRoleService.parent.lock(true) \/> ErrorResponse.serverError
      _ <- roleIdAvailable(newRole.roleId) \/> duplicateRoleIdError(newRole)
    yield
      val supportedRoleId = domainRoleService.addNewRole(newRole)
      newRole.copy(id = supportedRoleId)

  private def roleIdAvailable(roleId: String): Boolean =
    val folderId = rws.getRoleFolder
    rws.getRoleByRoleId(folderId, roleId) eq null

  private def duplicateRoleIdError(role: RoleDTO): ErrorResponse =
    ErrorResponse.validationError("roleId", role.roleId)("Duplicate role id.")
end DomainRolesRootApiImpl
