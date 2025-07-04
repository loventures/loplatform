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

package loi.authoring.web

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.project.{AccessRestriction, Project}
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}
import loi.cp.right.Right

import scala.reflect.ClassTag

@Service
trait AuthoringApiWebUtils:

  def workspaceOptionallyAtCommitOrThrow404(
    bronchId: Long,
    commitOpt: Option[Long],
    accessRestriction: AccessRestriction = AccessRestriction.projectMember,
    cache: Boolean = true
  ): AttachedReadWorkspace = commitOpt match
    case Some(commitId) => workspaceAtCommitOrThrow404(bronchId, commitId, accessRestriction, cache)
    case None           => workspaceOrThrow404(bronchId, accessRestriction, cache)

  def workspaceOrThrow404(
    bronchId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember,
    cache: Boolean = true
  ): AttachedReadWorkspace

  def workspaceAtCommitOrThrow404(
    bronchId: Long,
    commitId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember,
    cache: Boolean = true
  ): AttachedReadWorkspace

  def detachedWorkspaceOrThrow404(commitId: Long, cache: Boolean = true): ReadWorkspace

  /** @return
    *   fake branch of an authoringproject.id or real branch of an assetbranch.id
    */
  def branchOrFakeBranchOrThrow404(
    bronchId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember
  ): Branch

  /** @return
    *   fake branch of an authoringproject.id or real master branch of an assetproject.id
    */
  def masterOrFakeBranchOrThrow404(
    projectId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember
  ): Branch

  /** @return
    *   project of an authoringproject.id or assetproject.id
    */
  def projectOrThrow404(id: Long, accessRestriction: AccessRestriction): Project

  // kill
  def throw403ForNonProjectUserWithout[A <: Right: ClassTag](project: Project): Unit

  // kill
  def throw403ForNonProjectOwnerWithout[A <: Right: ClassTag](project: Project): Unit

  def nodeOrThrow404Typed[A: AssetType](workspace: ReadWorkspace, nodeNameStr: String): Asset[A]
end AuthoringApiWebUtils
