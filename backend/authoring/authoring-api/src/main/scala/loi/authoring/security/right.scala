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

package loi.authoring.security

import loi.cp.admin.right.{AdminRight, HostingAdminRight}
import loi.cp.right.RightBinding

object right:

  // we don't intend any roles to have this, this is to make rights appear near
  // each other in the Rights UI
  @RightBinding(
    name = "right.authoring.all.name",
    description = "right.authoring.all.description"
  )
  abstract class AllAuthoringActionsRight extends AdminRight

  @RightBinding(
    name = "right.authoring.appAccess.name",
    description = "right.authoring.appAccess.description"
  )
  abstract class AccessAuthoringAppRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.adminAppAccess.name",
    description = "right.authoring.adminAppAccess.description"
  )
  abstract class AccessAuthoringAdminAppRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.createProject.name",
    description = "right.authoring.createProject.description"
  )
  abstract class CreateProjectRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.editContributorsAnyProject.name",
    description = "right.authoring.editContributorsAnyProject.description"
  )
  abstract class EditContributorsAnyProjectRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.changeOwnerAnyProject.name",
    description = "right.authoring.changeOwnerAnyProject.description"
  )
  abstract class ChangeOwnerAnyProjectRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.deleteAnyProject.name",
    description = "right.authoring.deleteAnyProject.description"
  )
  abstract class DeleteAnyProjectRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.editSettingsAnyProject.name",
    description = "right.authoring.editSettingsAnyProject.description"
  )
  abstract class EditSettingsAnyProjectRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.addVersionAnyProject.name",
    description = "right.authoring.addVersionAnyProject.description"
  )
  abstract class AddVersionAnyProjectRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.publishOffering.name",
    description = "right.authoring.publishOffering.description"
  )
  abstract class PublishOfferingRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.viewAllProjects.name",
    description = "right.authoring.viewAllProjects.description"
  )
  abstract class ViewAllProjectsRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.editContentAnyProject.name",
    description = "right.authoring.editContentAnyProject.description"
  )
  abstract class EditContentAnyProjectRight extends AllAuthoringActionsRight

  @RightBinding(
    name = "right.authoring.copyAnyProjectVersion.name",
    description = "right.authoring.copyAnyProjectVersion.description"
  )
  abstract class CopyAnyProjectVersionRight extends HostingAdminRight

  @RightBinding(
    name = "right.authoring.viewActivityLog.name",
    description = "right.authoring.viewActivityLog.description"
  )
  abstract class ViewActivityLogRight extends AllAuthoringActionsRight
end right
