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

package loi.cp.domain

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.relationship.RelationshipConstants
import loi.cp.enrollment.*
import loi.cp.folder.{Folder, LightweightFolderService}
import loi.cp.item.LightweightItemService
import scaloi.syntax.collection.*

@Service
class EnrollmentInitializationService(
  itemService: LightweightItemService,
  folderService: LightweightFolderService,
  enrollmentService: LightweightEnrollmentService
):
  def initializeDomainRoleFolder(domainFolder: Folder): Unit =
    // Role Folder
    val domainFolderItem: Item =
      itemService
        .findItem(domainFolder.id)
        .getOrElse(throw new IllegalStateException(s"No domain folder for ${domainFolder.id}"))

    folderService.createFolder(
      Some(RelationshipConstants.ID_FOLDER_ROLES),
      Some(RelationshipConstants.ITEM_TYPE_ROLE),
      Some(RelationshipConstants.ITEM_TYPE_ROLE),
      domainFolderItem
    )
  end initializeDomainRoleFolder

  // See RoleStartupTask / RoleStartupOps / AdminRightUpgradeTask / OverlordRoleStartupTask

  def addDefaultRolesAndRights(domain: DomainDTO): Unit =
    val roles: Seq[Role]                = enrollmentService.getOrCreateRoles(StandardRoleName.values*)
    val roleByName: Map[RoleName, Role] = roles.groupUniqBy(_.roleName)

    def setSupportedDomainRole(
      roleName: RoleName,
      rightsSet: RightsSet
    ): Unit =
      val role: Role = roleByName(roleName)
      val granted    = rightsSet.enrollmentRights.collect({ case GrantRight(right) => right })
      val denied     = rightsSet.enrollmentRights.collect({ case DenyRight(right) => right })
      enrollmentService.setSupportedDomainRole(domain, role, granted.toSeq, denied.toSeq)

    SupportedRole.defaultDomainRightsByRoleName.foreach({ case (roleName, rightsSet) =>
      setSupportedDomainRole(roleName, rightsSet)
    })
  end addDefaultRolesAndRights
end EnrollmentInitializationService
