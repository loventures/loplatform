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

import loi.authoring.{AssetType, NodeId}
import loi.authoring.asset.Asset
import loi.authoring.edge.{EdgeElem, Group}
import loi.cp.asset.edge.EdgeData
import cats.syntax.option.*

import java.util.UUID

/** Operations to change assets.
  */
sealed trait WriteOp

/** @param exemptBlobName
  *   the blob name that the `data.source` can use to avoid blob name validation. This is like a pre-validated blob name
  *   from the far past before we prefixed all object keys with "authoring/". AddNodes used for copying set an
  *   exemptBlobName.
  */
case class AddNode[A](
  data: A,
  name: UUID = UUID.randomUUID(),
  exemptBlobName: Option[String] = None,
)(implicit val assetType: AssetType[A])
    extends WriteOp

object AddNode:

  def apply[A](assetType: AssetType[A])(data: A, name: UUID): AddNode[A] = AddNode(data, name)(using assetType)

case class DeleteNode(name: UUID) extends WriteOp

case class AddEdge(
  sourceName: UUID,
  targetName: UUID,
  group: Group,
  name: UUID = UUID.randomUUID(),
  position: Option[Position] = None,
  traverse: Boolean = true,
  data: EdgeData = EdgeData.empty,
  edgeId: UUID = UUID.randomUUID(),
  remote: Option[Long] = None,
) extends WriteOp

/** Deletes an edge.
  *
  * @param name
  *   the name of the edge to delete
  */
case class DeleteEdge(
  name: UUID,
) extends WriteOp

case class SetEdgeData(
  name: UUID,
  data: EdgeData,
  restoreEdge: Option[EdgeElem] = None
) extends WriteOp

case class SetEdgeOrder(
  sourceName: UUID,
  group: Group,
  ordering: Seq[UUID]
) extends WriteOp

case class SetNodeData[A](
  name: UUID,
  data: A,
  restoreNode: Option[NodeId] = None
)(implicit val assetType: AssetType[A])
    extends WriteOp

object SetNodeData:

  def apply[A](assetType: AssetType[A])(data: A, name: UUID): SetNodeData[A] = SetNodeData(name, data)(using assetType)

  def fromAsset[A](asset: Asset[A]): SetNodeData[A] =
    SetNodeData(asset.info.name, asset.data)(using asset.assetType)

  def restoreAssetData[A](asset: Asset[A]): SetNodeData[A] =
    SetNodeData(asset.info.name, asset.data, asset.info.id.some)(using asset.assetType)

case class SetHomeName(name: UUID) extends WriteOp

case class SetRootName(name: UUID) extends WriteOp

sealed trait Position

object Position:

  case object Start                 extends Position
  case object End                   extends Position
  case class Before(edgeName: UUID) extends Position
  case class After(edgeName: UUID)  extends Position

case class WriteOps(
  addNodes: Seq[AddNode[?]],
  deleteNodes: Seq[DeleteNode],
  setNodeDatas: Seq[SetNodeData[?]],
  addEdges: Seq[AddEdge],
  setEdgeDatas: Seq[SetEdgeData],
  deleteEdges: Seq[DeleteEdge],
  setEdgeOrders: Seq[SetEdgeOrder],
  setRootNames: Seq[SetRootName],
  setHomeNames: Seq[SetHomeName],
):
  lazy val addNodeNames: Set[UUID]     = addNodes.view.map(_.name).toSet
  lazy val setEdgeDataNames: Set[UUID] = setEdgeDatas.view.map(_.name).toSet
  lazy val deleteEdgeNames: Set[UUID]  = deleteEdges.view.map(_.name).toSet
end WriteOps

object WriteOps:
  def from(ops: Seq[WriteOp]): WriteOps =

    val addNodes      = List.newBuilder[AddNode[?]]
    val deleteNodes   = List.newBuilder[DeleteNode]
    val setNodeDatas  = List.newBuilder[SetNodeData[?]]
    val deleteEdges   = List.newBuilder[DeleteEdge]
    val addEdges      = List.newBuilder[AddEdge]
    val setEdgeOrders = List.newBuilder[SetEdgeOrder]
    val setEdgeDatas  = List.newBuilder[SetEdgeData]
    val setRootNames  = List.newBuilder[SetRootName]
    val setHomeNames  = List.newBuilder[SetHomeName]

    ops.foreach {
      case op: AddNode[?]     => addNodes += op
      case op: DeleteNode     => deleteNodes += op
      case op: SetNodeData[?] => setNodeDatas += op
      case op: DeleteEdge     => deleteEdges += op
      case op: AddEdge        => addEdges += op
      case op: SetEdgeOrder   => setEdgeOrders += op
      case op: SetEdgeData    => setEdgeDatas += op
      case op: SetRootName    => setRootNames += op
      case op: SetHomeName    => setHomeNames += op
    }

    WriteOps(
      addNodes.result(),
      deleteNodes.result(),
      setNodeDatas.result(),
      addEdges.result(),
      setEdgeDatas.result(),
      deleteEdges.result(),
      setEdgeOrders.result(),
      setRootNames.result(),
      setHomeNames.result()
    )
  end from
end WriteOps
