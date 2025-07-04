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

package loi.cp.course

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.project.AccessRestriction
import loi.authoring.workspace.AttachedReadWorkspace
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.course.lightweight.Lwc

@Service
class CourseWorkspaceService(
  readWorkspaceService: ReadWorkspaceService
):

  def loadReadWorkspace(section: CourseSection): AttachedReadWorkspace =
    readWorkspaceService.requireReadWorkspaceAtCommit(section.branch.id, section.commitId, AccessRestriction.none)

  def loadReadWorkspace(lwc: Lwc): AttachedReadWorkspace =
    readWorkspaceService.requireReadWorkspaceAtCommit(lwc.branch.id, lwc.commitId, AccessRestriction.none)

  def requireReadWorkspaceAtCommit(section: CourseSection, commitId: Long): AttachedReadWorkspace =
    readWorkspaceService.requireReadWorkspaceAtCommit(section.branch.id, commitId, AccessRestriction.none)
end CourseWorkspaceService
