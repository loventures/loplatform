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
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.edge.BaseEdgeService
import loi.authoring.project.*
import loi.authoring.security.right.EditContentAnyProjectRight
import loi.authoring.write.LayeredWriteWorkspace
import loi.cp.user.UserService

@Service
class WorkspaceService(
  commitDao2: CommitDao2,
  projectDao: ProjectDao2,
  edgeService: BaseEdgeService,
  userService: UserService,
  userDto: => UserDTO,
):
  def loadWriteWorkspace(
    bronchId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMemberOr[EditContentAnyProjectRight]
  ): Option[WriteWorkspace] =
    for
      project <- projectDao.loadForUpdate(bronchId)
      if accessRestriction.pass(userDto, project, userService)
    yield
      val headEntity = commitDao2.loadWithInitializedDocs(project.head)
      val doc        = headEntity.comboDoc
      val edges      = edgeService.loadEdgeAttrs(doc)

      val depCommitIds       = doc.deps.values.map(_.commitId)
      val initializedCommits = commitDao2.loadWithInitializedDocs(depCommitIds)

      val fuzzyLayerBases = for
        (projectId, depInfo) <- doc.deps
        initializedCommit    <- initializedCommits.get(depInfo.commitId)
      yield (projectId, initializedCommit.comboDoc.toFuzzyLayerBase)

      new LayeredWriteWorkspace(project, doc, edges, fuzzyLayerBases)
  end loadWriteWorkspace

  def requireWriteWorkspace(
    bronchId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMemberOr[EditContentAnyProjectRight]
  ): WriteWorkspace =
    loadWriteWorkspace(bronchId, accessRestriction).getOrElse(
      throw new NoSuchElementException(s"branch or project $bronchId")
    )

  def loadReadWorkspace(
    bronchId: Long,
    accessRestriction: AccessRestriction,
    cache: Boolean
  ): Option[AttachedReadWorkspace] =
    loadLayeredRead(bronchId, accessRestriction)

  def loadLayeredRead(
    projectId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember,
  ): Option[ProjectWorkspace] = for
    project <- projectDao.load(projectId)
    if accessRestriction.pass(userDto, project, userService)
  yield

    val headEntity = commitDao2.loadWithInitializedDocs(project.head)
    val doc        = headEntity.comboDoc
    val edges      = edgeService.loadEdgeAttrs(doc)
    new ProjectWorkspace(project, headEntity.toCommit, doc, edges)

  def loadLayeredReadAtCommit(
    projectId: Long,
    commitId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember,
  ): Option[ProjectWorkspace] = for
    project <- projectDao.load(projectId)
    if accessRestriction.pass(userDto, project, userService)
    commit  <- commitDao2.loadWithInitializedDocs(commitId)
  yield
    val doc   = commit.comboDoc
    val edges = edgeService.loadEdgeAttrs(doc)
    new ProjectWorkspace(project, commit.toCommit, doc, edges)

  def requireReadWorkspace(
    bronchId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember,
    cache: Boolean = false
  ): AttachedReadWorkspace =
    loadReadWorkspace(bronchId, accessRestriction, cache).getOrElse(
      throw new NoSuchElementException(s"branch or project $bronchId")
    )

  def requireLayeredRead(
    projectId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember
  ): ProjectWorkspace =
    loadLayeredRead(projectId, accessRestriction).getOrElse(
      throw new NoSuchElementException(s"project $projectId)")
    )

  def requireLayeredReadAtCommit(
    projectId: Long,
    commitId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember
  ): ProjectWorkspace =
    loadLayeredReadAtCommit(projectId, commitId, accessRestriction).getOrElse(
      throw new NoSuchElementException(s"project $projectId; commit $commitId")
    )

  def loadReadWorkspaceAtCommit(
    bronchId: Long,
    commitId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember,
    cache: Boolean = false
  ): Option[AttachedReadWorkspace] =
    for
      project      <- projectDao.load(bronchId)
      if accessRestriction.pass(userDto, project, userService)
      commitEntity <- commitDao2.loadWithInitializedDocs(commitId)
    yield
      val doc   = commitEntity.comboDoc
      val edges = edgeService.loadEdgeAttrs(doc)
      new ProjectWorkspace(project, commitEntity.toCommit, doc, edges)
  end loadReadWorkspaceAtCommit

  def requireReadWorkspaceAtCommit(
    bronchId: Long,
    commitId: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember,
    cache: Boolean = false
  ): AttachedReadWorkspace =
    loadReadWorkspaceAtCommit(bronchId, commitId, accessRestriction, cache).getOrElse(
      throw new NoSuchElementException(s"branch or project $bronchId; commit $commitId")
    )

  def loadDetachedWorkspace(commitId: Long, cache: Boolean = false): Option[ReadWorkspace] =
    commitDao2.loadWithInitializedDocs(commitId) map { commitEntity =>
      val doc   = commitEntity.comboDoc
      val edges = edgeService.loadEdgeAttrs(doc)
      new LayeredWorkspace(commitEntity.toCommit, doc, edges)
    }
  end loadDetachedWorkspace

  def requireDetachedWorkspace(commitId: Long, cache: Boolean = false): ReadWorkspace =
    loadDetachedWorkspace(commitId, cache).getOrElse(throw new NoSuchElementException(s"commit $commitId"))

end WorkspaceService
