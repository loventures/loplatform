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

import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.operation.UncheckedVoidOperation
import com.learningobjects.cpxp.util.{GuidUtil, ManagedUtils}
import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.asset.Asset
import loi.authoring.copy.store.CopyReceiptDao
import loi.authoring.edge.EdgeService
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.{AttachedReadWorkspace, TraversalScope}
import loi.authoring.write.{ValidatedAddEdge, ValidatedAddNode}
import loi.cp.i18n.AuthoringBundle
import org.log4s.Logger

import java.util.UUID

/** @author
  *   mkalish
  */
abstract class CopyOperation(
  copyReceiptDao: CopyReceiptDao,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  srcWorkspace: AttachedReadWorkspace,
  val receipt: CopyReceipt,
  newTitle: String,
) extends UncheckedVoidOperation:

  // this is updated as a side effect of the copy so that whenever an unexpected
  // exception occurs, we can write the most accurate receipt ot the database
  private[copy] var tappedReceipt: Option[CopyReceipt] = None

  private[copy] val log: Logger = org.log4s.getLogger

  private[copy] def copyNodes(state: CopyState, scope: TraversalScope): CopyState =

    val batches = scope.nodeIds.grouped(1000).toSeq
    batches.foldLeft(state)({ case (acc, batch) =>
      val nodes     = nodeService.load(srcWorkspace).byId(batch.keys)
      val nodesById = nodes.groupBy(_.info.id).view.mapValues(_.head).toMap

      val nextAcc = batch.foldLeft(acc)({
        case (acc2, (nodeId, true))  =>
          // `true` means the nodeId is for a competency-like node
          // we don't copy them
          val node = nodesById(nodeId)
          acc2.copy(
            copiedNodes = acc2.copiedNodes.updated(nodeId, node.info.name)
          )
        case (acc2, (nodeId, false)) =>
          val node      = nodesById(nodeId)
          val addNodeOp = copyNode(node)
          acc2.copy(
            copiedNodes = acc2.copiedNodes.updated(nodeId, addNodeOp.name),
            addNodeOps = acc2.addNodeOps :+ addNodeOp
          )
      })

      nextAcc.markProgress(batch.size)
    })
  end copyNodes

  private[copy] def copyEdges(state: CopyState, scope: TraversalScope): CopyState =
    val batches = scope.nodeIds.grouped(1000).toSeq
    batches.foldLeft(state)({ case (acc, batch) =>
      val edgeIds = batch
        .filter(_._2 == false) // only want content-nodes, false means content node
        .keys
        .flatMap(srcWorkspace.outEdgeInfos)
        .map(_.id)

      val edges = edgeService.load(srcWorkspace).byId(edgeIds)

      val addEdgeOps = edges.map(edge =>
        ValidatedAddEdge(
          UUID.randomUUID(),
          state.copiedNodes(edge.source.info.id),
          state.copiedNodes(edge.target.info.id),
          edge.group,
          edge.position,
          edge.traverse,
          edge.data,
          edge.edgeId,
          addDurableEdge = true,
        )
      )

      acc.copy(addEdgeOps = acc.addEdgeOps ++ addEdgeOps).markProgress(addEdgeOps.size)
    })
  end copyEdges

  private[copy] def copyNode[A](
    node: Asset[A],
  ): ValidatedAddNode[A] =

    val newData = if node.info.id == receipt.originalId then

      val rtNode = node.receiveTitle(newTitle)
      val rtData = rtNode.data

      // set the title in the JSON via the back door
      if node.assetType.specialProps.title then
        val json = JacksonUtils.getFinatraMapper.valueToTree[ObjectNode](rtData)
        json.put("title", newTitle)
        JacksonUtils.getFinatraMapper.treeToValue(json, node.assetType.dataClass)
      else rtData
    else node.data

    ValidatedAddNode(UUID.randomUUID(), newData)(using node.assetType)
  end copyNode

  // extracts the receipt as the CopyState changes and stores it on this operation
  // such that any unexpected exception will allow us to dump the most accurate receipt
  // possible to the database
  private[copy] def tapReceipt(f: => CopyState): CopyState =
    val nextState = f
    tappedReceipt = Some(nextState.receipt)
    nextState

  private[copy] def recordFailure(ex: Throwable): Throwable =

    val error = ex match
      case ume: UncheckedMessageException => ume.getErrorMessage
      case exp: Exception                 =>
        val msg = AuthoringBundle.message("copy.fatalException", GuidUtil.errorGuid(), exp.getMessage)
        log.warn(msg.value)
        log.warn(exp)("Copy Failure")
        msg

    val failedReceipt = tappedReceipt.getOrElse(receipt).addError(error).markFailure()
    copyReceiptDao.update(failedReceipt)
    ManagedUtils.commit()

    ex
  end recordFailure
end CopyOperation
