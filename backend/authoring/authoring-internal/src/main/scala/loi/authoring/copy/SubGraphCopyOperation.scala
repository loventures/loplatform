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

import loi.authoring.asset.Asset
import loi.authoring.copy.store.CopyReceiptDao
import loi.authoring.edge.EdgeService
import loi.authoring.node.AssetNodeService
import loi.authoring.project.AccessRestriction
import loi.authoring.workspace.{AttachedReadWorkspace, TraversalScope, WorkspaceService}
import loi.authoring.write.BaseWriteService

import scala.util.control.NonFatal

class SubGraphCopyOperation(
  copyReceiptDao: CopyReceiptDao,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  workspaceService: WorkspaceService,
  writeService: BaseWriteService,
  receipt: CopyReceipt,
  srcWorkspace: AttachedReadWorkspace,
  node: Asset[?],
  newTitle: String,
) extends CopyOperation(copyReceiptDao, nodeService, edgeService, srcWorkspace, receipt, newTitle):

  override protected def execute(): Unit =

    try executeUnsafe()
    catch case NonFatal(ex) => throw recordFailure(ex)

  private def executeUnsafe(): Unit =

    val startState       = tapReceipt(CopyState.empty(receipt.markStart()))
    val scope            = TraversalScope.build(srcWorkspace, Set(node.info.id))
    val copiedNodesState = tapReceipt(copyNodes(startState, scope))
    val copiedEdgesState = tapReceipt(copyEdges(copiedNodesState, scope))

    val ww     = workspaceService.requireWriteWorkspace(srcWorkspace.bronchId, AccessRestriction.none)
    val result = writeService.commitForWorkspaceCopy(
      ww,
      Set(node.info.name),
      copiedEdgesState.addNodeOps,
      copiedEdgesState.addEdgeOps
    )

    val targetId       = result.ws.requireNodeId(copiedEdgesState.copiedNodes(receipt.originalId)).get
    val successReceipt = copiedEdgesState.receipt.markSuccess(targetId)

    copyReceiptDao.update(successReceipt)
  end executeUnsafe
end SubGraphCopyOperation
