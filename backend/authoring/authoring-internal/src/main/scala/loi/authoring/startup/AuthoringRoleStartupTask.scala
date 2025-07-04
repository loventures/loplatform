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

package loi.authoring.startup

import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.relationship.RelationshipWebService
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding, StartupTaskScope}
import loi.authoring.security.right.*
import loi.authoring.security.role.*
import loi.cp.admin.right.ViewLtiToolRight
import loi.cp.role.{RoleService, RoleStartupOps}

@StartupTaskBinding(
  version = 5,
  taskScope = StartupTaskScope.Domain
)
class AuthoringRoleStartupTask(
  override val domain: DomainDTO,
  override val rs: RoleService,
  override val rws: RelationshipWebService
) extends StartupTask
    with RoleStartupOps:

  override def run(): Unit =

    putRoles(CurriculumProjectAdminRoleId, CurriculumDeveloperRoleId)

    putSupportedRolesInDomain(
      SupportedRoleInfo(
        CurriculumProjectAdminRoleName,
        List(
          classOf[AccessAuthoringAppRight],
          classOf[CreateProjectRight],
          classOf[EditContributorsAnyProjectRight],
          classOf[EditSettingsAnyProjectRight],
          classOf[AddVersionAnyProjectRight],
          classOf[PublishOfferingRight],
          classOf[ViewAllProjectsRight],
          classOf[EditContentAnyProjectRight],
          classOf[ViewLtiToolRight]
        )
      ),
      SupportedRoleInfo(
        CurriculumDeveloperRoleName,
        List(
          classOf[AccessAuthoringAppRight],
          classOf[CreateProjectRight],
          classOf[ViewLtiToolRight]
        )
      )
    )
  end run
end AuthoringRoleStartupTask
