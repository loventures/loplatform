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

package loi.authoring.copy

import cats.syntax.option.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.copy.store.CopyReceiptDao
import loi.authoring.edge.EdgeService
import loi.authoring.node.AssetNodeService
import loi.authoring.project.*
import loi.authoring.workspace.{AttachedReadWorkspace, TraversalScope, WorkspaceService}
import loi.authoring.write.BaseWriteService

import scala.util.control.NonFatal

class ProjectCopyOperation(
  copyReceiptDao: CopyReceiptDao,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  projectService: BaseProjectService,
  workspaceService: WorkspaceService,
  writeService: BaseWriteService,
  receipt: CopyReceipt,
  srcWorkspace: AttachedReadWorkspace,
  newTitle: String,
  destinationDomain: DomainDTO,
  userDTO: UserDTO
) extends CopyOperation(copyReceiptDao, nodeService, edgeService, srcWorkspace, receipt, newTitle):

  override protected def execute(): Unit =
    try
      val startingNodeIds  = srcWorkspace.rootIds.values.toSet
      val startState       = tapReceipt(CopyState.empty(receipt.markStart()))
      val scope            = TraversalScope.build(srcWorkspace, startingNodeIds, includeNonTraverse = true)
      val copiedNodesState = tapReceipt(copyNodes(startState, scope))
      val copiedEdgesState = tapReceipt(copyEdges(copiedNodesState, scope))

      val projectRootId = srcWorkspace.nodeId(srcWorkspace.rootName)
      val homeNodeId    = srcWorkspace.nodeId(srcWorkspace.homeName)

      val newRootNodeName = copiedEdgesState.copiedNodes(projectRootId)
      val newHomeNodeName = copiedEdgesState.copiedNodes(homeNodeId)

      val tgtProject = projectService
        .insertProject(
          CreateProjectDto(newTitle, ProjectType.Course, userDTO.id, layered = srcWorkspace.layered),
          destinationDomain.id,
          newRootNodeName.some,
          newHomeNodeName.some
        )
        .valueOr(errors => throw new UncheckedMessageException(errors.head))

      val tgtWorkspace  = workspaceService.requireWriteWorkspace(tgtProject.id, AccessRestriction.none)
      val result        = writeService.commitForWorkspaceCopy(
        tgtWorkspace,
        destinationDomain,
        srcWorkspace.rootIds.keySet,
        copiedEdgesState.addNodeOps,
        copiedEdgesState.addEdgeOps
      )
      val newRootNodeId = result.ws.requireNodeId(newRootNodeName).get

      val successReceipt = copiedEdgesState.receipt.markSuccess(newRootNodeId)
      copyReceiptDao.update(successReceipt)
    catch case NonFatal(ex) => throw recordFailure(ex)
end ProjectCopyOperation
