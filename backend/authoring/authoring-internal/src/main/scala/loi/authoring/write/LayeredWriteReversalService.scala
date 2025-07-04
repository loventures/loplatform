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

package loi.authoring.write

import cats.syntax.either.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import clots.syntax.boolean.*
import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.edge.Group
import loi.authoring.edge.store.{EdgeDao2, EdgeEntity2}
import loi.authoring.node.store.{NodeDao2, NodeEntity2}
import loi.authoring.project.BigCommit
import loi.authoring.workspace.{ReadWorkspace, WorkspaceService}
import loi.authoring.write.ReversingError.{EdgeConflict, IrreversibleCommit, IrreversibleOpType, NodeConflict}
import loi.authoring.write.store.{DbWriteOp, DbAddEdge, DbDeleteEdge, DbSetEdgeData, DbSetNodeData, DbAddNode}
import loi.cp.asset.edge.EdgeData
import loi.jackson.syntax.jsonNode.*

import java.util.UUID

// because LayeredWriteService is already very large and the reversal code would be an island there.
@Service
class LayeredWriteReversalService(
  edgeDao2: EdgeDao2,
  nodeDao2: NodeDao2,
  workspaceService: WorkspaceService,
):
  import LayeredWriteReversalService.*

  /** Builds `List[WriteOp]` that reverse `commit`.
    */
  // not a general tool, can only reverse specific kinds of commit - like those made in realtime mode.
  // a general reverser would be as complex as general commit (that is, it would need pending state as it iterated the ops in reverse)
  def reverseWriteOps(ws: LayeredWriteWorkspace, commit: BigCommit): Either[ReversingError, List[WriteOp]] =
    val ops    = commit.ops.finatraDecoded[List[DbWriteOp]]
    val parent = commit.parentId.flatMap(workspaceService.loadReadWorkspaceAtCommit(ws.bronchId, _))

    for
      // _            <- (ops.size > 1).thenRaise(irreversibleCommit)
      pendingOps   <- getPendingReversals(ops, commit, parent, ws)
      parentNodeIds = pendingOps.view.flatMap(_.parentNodeIds)
      parentEdgeIds = pendingOps.view.flatMap(_.parentEdgeIds)
      parentNodes   = nodeDao2.load(parentNodeIds).view.map(n => n.name -> n).toMap
      parentEdges   = edgeDao2.load(parentEdgeIds).view.map(e => e.name -> e).toMap
      ops          <- getReverseOps(pendingOps, parentNodes, parentEdges)
    yield ops
  end reverseWriteOps

  private def getPendingReversals(
    ops: List[DbWriteOp],
    commit: BigCommit,
    parent: Option[ReadWorkspace],
    head: ReadWorkspace,
  ): Either[ReversingError, List[PendingReversal]] = ops.flatTraverse {
    case op: DbAddNode     => List.empty.asRight
    case op: DbSetNodeData => getSetNodeDataPendingReversal(op, commit, parent, head)
    case op: DbSetEdgeData => getSetEdgeDataPendingReversal(op, commit, parent, head)
    case op: DbAddEdge     => List(AddEdgePendingReversal(op)).asRight
    case op: DbDeleteEdge  => getDeleteEdgePendingReversal(op, parent)
    case op                => IrreversibleOpType(op.getClass.getSimpleName).asLeft
  }

  private def getSetNodeDataPendingReversal(
    snd: DbSetNodeData,
    prey: BigCommit,
    parent: Option[ReadWorkspace],
    head: ReadWorkspace,
  ): Either[ReversingError, List[PendingReversal]] = for
    parent0  <- parent.toRight(noParentCommit)
    idPrey   <- prey.comboDoc.getNodeId(snd.name).toRight(noSuchNode(snd.name, prey.id))
    idHead    = head.getNodeId(snd.name)
    _        <- idHead.exists(_ != idPrey).thenRaise(nodeConflict(snd.name))
    idParent <- parent0.getNodeId(snd.name).toRight(noSuchNode(snd.name, parent0.commitId))
  yield
    if idHead.isEmpty then Nil // because the node is already gone there is nothing to set
    else List(SetNodeDataPendingReversal(snd, parent0, idParent))

  private def getSetEdgeDataPendingReversal(
    sed: DbSetEdgeData,
    prey: BigCommit,
    parent: Option[ReadWorkspace],
    head: ReadWorkspace
  ): Either[ReversingError, List[PendingReversal]] = for
    parent0  <- parent.toRight(noParentCommit)
    idPrey   <- prey.comboDoc.getFuzzyEdgeId(sed.name).toRight(noSuchEdge(sed.name, prey.id))
    idHead    = head.getEdgeId(sed.name)
    _        <- idHead.exists(_ != idPrey).thenRaise(edgeConflict(sed.name))
    idParent <- parent0.getEdgeId(sed.name).toRight(noSuchEdge(sed.name, parent0.commitId))
  yield
    if idHead.isEmpty then Nil // because the edge is already gone there is nothing to set
    else List(SetEdgeDataPendingReversal(sed, parent0, idParent))

  private def getDeleteEdgePendingReversal(
    de: DbDeleteEdge,
    parent: Option[ReadWorkspace],
  ): Either[ReversingError, List[PendingReversal]] = for
    parent0 <- parent.toRight(noParentCommit)
    idPrey  <- parent0.getEdgeId(de.name).toRight(noSuchEdge(de.name, parent0.commitId))
  yield List(DeleteEdgePendingReversal(de, parent0, idPrey))

  private def getReverseOps(
    ops: List[PendingReversal],
    parentNodes: Map[UUID, NodeEntity2],
    parentEdges: Map[UUID, EdgeEntity2]
  ): Either[ReversingError, List[WriteOp]] = ops.traverse {
    case op: SetNodeDataPendingReversal => getSetNodeDataReverseOp(op, parentNodes)
    case op: SetEdgeDataPendingReversal => getSetEdgeDataReverseOp(op, parentEdges)
    case op: AddEdgePendingReversal     => DeleteEdge(op.op.name).asRight[ReversingError]
    case op: DeleteEdgePendingReversal  => getDeleteEdgeReverseOp(op, parentEdges)
  }

  private def getSetNodeDataReverseOp(
    pendingReversal: SetNodeDataPendingReversal,
    parentNodes: Map[UUID, NodeEntity2]
  ): Either[ReversingError, WriteOp] = for parentNode <-
      parentNodes.get(pendingReversal.op.name).toRight(noSuchNode(pendingReversal))
  yield SetNodeData.restoreAssetData(parentNode.toAsset)

  private def getSetEdgeDataReverseOp(
    pendingReversal: SetEdgeDataPendingReversal,
    parentEdges: Map[UUID, EdgeEntity2]
  ): Either[ReversingError, WriteOp] = for parentEdge <-
      parentEdges.get(pendingReversal.op.name).toRight(noSuchEdge(pendingReversal))
  yield SetEdgeData(parentEdge.name, parentEdge.data.finatraDecoded[EdgeData], parentEdge.toEdgeElem.some)

  private def getDeleteEdgeReverseOp(
    pendingReversal: DeleteEdgePendingReversal,
    parentEdges: Map[UUID, EdgeEntity2]
  ): Either[ReversingError, WriteOp] = for parentEdge <-
      parentEdges.get(pendingReversal.op.name).toRight(noSuchEdge(pendingReversal))
  yield

    val grp = Group.withName(parentEdge.group)

    val memberNames =
      pendingReversal.parentWs.outEdgeAttrs(parentEdge.sourceName, grp).toSeq.sortBy(_.position).map(_.name)
    val memberIndex = memberNames.indexOf(parentEdge.name)
    val prec        = memberIndex - 1
    val pos         = if prec < 0 then Position.Start else Position.After(memberNames(prec))

    val data = parentEdge.data.finatraDecoded[EdgeData]

    AddEdge(
      parentEdge.sourceName,
      parentEdge.targetName,
      grp,
      parentEdge.name,
      pos.some,
      parentEdge.traverse,
      data,
      parentEdge.localId,
      None
    )
end LayeredWriteReversalService

object LayeredWriteReversalService:
  private def noSuchNode(pendingReversal: SetNodeDataPendingReversal): ReversingError =
    noSuchNode(pendingReversal.op.name, pendingReversal.parentWs.commitId)

  private def noSuchEdge(pendingReversal: SetEdgeDataPendingReversal): ReversingError =
    noSuchEdge(pendingReversal.op.name, pendingReversal.parentWs.commitId)

  private def noSuchEdge(pendingReversal: DeleteEdgePendingReversal): ReversingError =
    noSuchEdge(pendingReversal.op.name, pendingReversal.parentWs.commitId)

  // wide constructors to help type inference
  private val irreversibleCommit: ReversingError                     = IrreversibleCommit
  private def nodeConflict(name: UUID): ReversingError               = NodeConflict(name)
  private def edgeConflict(name: UUID): ReversingError               = EdgeConflict(name)
  private val noParentCommit: ReversingError                         = ReversingError.NoParentCommit
  private def noSuchNode(name: UUID, commitId: Long): ReversingError = ReversingError.NoSuchNode(name, commitId)
  private def noSuchEdge(name: UUID, commitId: Long): ReversingError = ReversingError.NoSuchEdge(name, commitId)

  sealed trait PendingReversal:
    def parentNodeIds: List[Long]
    def parentEdgeIds: List[Long]

  final case class SetNodeDataPendingReversal(
    op: DbSetNodeData,
    parentWs: ReadWorkspace,
    parentNodeId: Long
  ) extends PendingReversal:
    override val parentNodeIds: List[Long] = List(parentNodeId)
    override val parentEdgeIds: List[Long] = Nil

  final case class SetEdgeDataPendingReversal(
    op: DbSetEdgeData,
    parentWs: ReadWorkspace,
    parentEdgeId: Long
  ) extends PendingReversal:
    override val parentNodeIds: List[Long] = Nil
    override val parentEdgeIds: List[Long] = List(parentEdgeId)

  // what one has to do when there aren't union types
  final case class AddEdgePendingReversal(op: DbAddEdge) extends PendingReversal:
    override val parentNodeIds: List[Long] = Nil
    override val parentEdgeIds: List[Long] = Nil

  final case class DeleteEdgePendingReversal(op: DbDeleteEdge, parentWs: ReadWorkspace, parentEdgeId: Long)
      extends PendingReversal:
    override val parentNodeIds: List[Long] = Nil
    override val parentEdgeIds: List[Long] = List(parentEdgeId)
end LayeredWriteReversalService
