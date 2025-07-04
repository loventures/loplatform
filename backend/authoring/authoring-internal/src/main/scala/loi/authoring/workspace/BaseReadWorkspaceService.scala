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

package loi.authoring.workspace

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.project.AccessRestriction
import loi.authoring.workspace.service.ReadWorkspaceService

@Service
class BaseReadWorkspaceService(
  workspaceService: WorkspaceService,
) extends ReadWorkspaceService:

  override def requireReadWorkspace(bronchId: Long, accessRestriction: AccessRestriction): AttachedReadWorkspace =
    workspaceService.requireReadWorkspace(bronchId, accessRestriction, cache = true)

  override def loadReadWorkspaceFromCommit(commitId: Long): Option[ReadWorkspace] =
    workspaceService.loadDetachedWorkspace(commitId, cache = true)

  override def requireDetachedWorkspace(commitId: Long): ReadWorkspace =
    workspaceService.requireDetachedWorkspace(commitId, cache = true)

  override def requireReadWorkspaceAtCommit(
    bronchId: Long,
    commitId: Long,
    accessRestriction: AccessRestriction
  ): AttachedReadWorkspace =
    workspaceService.requireReadWorkspaceAtCommit(bronchId, commitId, accessRestriction, cache = true)
end BaseReadWorkspaceService
