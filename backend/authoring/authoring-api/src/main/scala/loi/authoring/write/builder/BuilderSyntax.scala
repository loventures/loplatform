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

package loi.authoring.write.builder

import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.edge.Group
import loi.cp.asset.edge.EdgeData

import java.util.UUID
import scala.language.implicitConversions

trait BuilderSyntax:

  implicit final def tgtData2Ops[B: AssetType](tgtData: B): TargetAddOps[B] = new TargetAddOps(tgtData)
  implicit final def tgtNode2Ops[B](tgt: NodeName[B]): TargetOps[B]         = new TargetOps(tgt)
  implicit final def tgtAsset2Ops[B](tgt: Asset[B]): TargetOps[B]           = new TargetOps(
    new NodeName[B](tgt.info.name)(using tgt.assetType)
  )

  implicit final def srcData2Ops[A: AssetType](srcData: A): SourceAddOps[A] = new SourceAddOps(srcData)
  implicit final def srcNode2Ops[A](a: NodeName[A]): SourceOps[A]           = new SourceOps(a)
  implicit final def srcAsset2Ops[A: AssetType](a: Asset[A]): SourceOps[A]  = new SourceOps(new NodeName(a.info.name))
end BuilderSyntax

/** Extension methods to configure an AddEdge WriteOp that SourceOps or SourceAddOps will add to the builder state when
  * the target of that AddEdge is not already in the builder state. An AddNode WriteOp will will be created for the
  * target of the AddEdge.
  */
final class TargetAddOps[B: AssetType](tgtData: B):
  def %(grp: Group): TargetAddArgs[B] = TargetAddArgs(tgtData, grp)

/** Extension methods to configure an AddEdge WriteOp that SourceOps or SourceAddOps will add to the builder state when
  * the target of that AddEdge is already in the builder state.
  */
final class TargetOps[B](private val tgt: NodeName[B]) extends AnyVal:
  def %(grp: Group): TargetArgs[B] = TargetArgs(tgt, grp)

/** Extension methods to add one AddEdge WriteOp to the builder state when the source of that AddEdge is not already in
  * the builder state. An AddNode WriteOp will be created for the source of the AddEdge.
  *
  * Double arrowheads yield all the new elements added to the builder state (one or both vertices and the edge).
  *
  * Single arrowheads yield only the added edge.
  */
final class SourceAddOps[A: AssetType](srcData: A):

  // add source, add target, add edge, yield new elements
  def ->>[B](tgtArgs: TargetAddArgs[B]): BuilderState[(NodeName[A], NodeName[B], EdgeName[A, B])] = for
    src <- addNode(srcData)
    tgt <- addNode(tgtArgs.nodeData)(using tgtArgs.assetType)
    e   <- addEdge(src, tgt, tgtArgs.grp)
  yield (src, tgt, e)

  // add source, add edge, yield new elements
  def ->>[B](tgtArgs: TargetArgs[B]): BuilderState[(NodeName[A], EdgeName[A, B])] = for
    src <- addNode(srcData)
    e   <- addEdge(src, tgtArgs.node, tgtArgs.grp)
  yield (src, e)

  // same operations as above but uses all default options for the edge, n.b. Group.Elements
  def ->>[B: AssetType](tgtData: B): BuilderState[(NodeName[A], NodeName[B], EdgeName[A, B])] = ->>(
    TargetAddArgs(tgtData)
  )
  def ->>[B](tgt: NodeName[B]): BuilderState[(NodeName[A], EdgeName[A, B])]                   = ->>(TargetArgs(tgt))
  def ->>[B](tgt: Asset[B]): BuilderState[(NodeName[A], EdgeName[A, B])]                      = ->>(
    TargetArgs(new NodeName(tgt.name)(using tgt.assetType))
  )

  // same operations as above except only the new edge is yielded
  def -->[B](tgtArgs: TargetAddArgs[B]): BuilderState[EdgeName[A, B]] = ->>(tgtArgs).map(_._3)
  def -->[B](tgtArgs: TargetArgs[B]): BuilderState[EdgeName[A, B]]    = ->>(tgtArgs).map(_._2)
  def -->[B: AssetType](tgtData: B): BuilderState[EdgeName[A, B]]     = ->>(tgtData).map(_._3)
  def -->[B](tgt: NodeName[B]): BuilderState[EdgeName[A, B]]          = ->>(tgt).map(_._2)
end SourceAddOps

/** Extension methods to add one AddEdge WriteOp to the builder state when the source of that AddEdge is already in the
  * builder state.
  */
final class SourceOps[A](private val src: NodeName[A]) extends AnyVal:

  // add target, add edge, yield new elements
  def ->>[B](tgtArgs: TargetAddArgs[B]): BuilderState[(NodeName[B], EdgeName[A, B])] = for
    tgt <- addNode(tgtArgs.nodeData)(using tgtArgs.assetType)
    e   <- addEdge(src, tgt, tgtArgs.grp)
  yield (tgt, e)

  // add edge, yield new edge, n.b. there is no double arrowhead version of this as there is only one new elem
  def -->[B](tgtArgs: TargetArgs[B]): BuilderState[EdgeName[A, B]] = addEdge(src, tgtArgs.node, tgtArgs.grp)

  // same operation as above but uses all default options for the edge, n.b. Group.Elements
  def ->>[B: AssetType](tgtData: B): BuilderState[(NodeName[B], EdgeName[A, B])] = ->>(TargetAddArgs(tgtData))

  // same operations as above except only the new edge is yielded
  def -->[B](tgtArgs: TargetAddArgs[B]): BuilderState[EdgeName[A, B]]     = ->>(tgtArgs).map(_._2)
  def -->[B: AssetType](tgtData: B): BuilderState[EdgeName[A, B]]         = -->(TargetAddArgs(tgtData))
  def -->[B: AssetType](tgtAsset: Asset[B]): BuilderState[EdgeName[A, B]] = -->(
    TargetArgs(new NodeName(tgtAsset.info.name))
  )
  def -->[B](tgt: NodeName[B]): BuilderState[EdgeName[A, B]]              = -->(TargetArgs(tgt))
end SourceOps

/** Configures the edge that SourceOps or SourceAddOps will add to the builder state. Indicates to Source[Add]Ops that
  * the target of the edge to add is already in the builder state.
  */
final case class TargetArgs[B](
  node: NodeName[B],
  grp: Group = Group.Elements,
  name: UUID = UUID.randomUUID(),
  traverse: Boolean = true,
  data: EdgeData = EdgeData.empty,
  edgeId: UUID = UUID.randomUUID(),
  remote: Option[Long] = None,
)

/** Configures the edge that SourceOps or SourceAddOps will add to the builder state. Indicates to Source[Add]Ops that
  * the target of the edge is not already in the builder state and that an AddNode WriteOp should be accumulated in
  * addition to the AddEdge WriteOp.
  */
final case class TargetAddArgs[B](
  nodeData: B,
  grp: Group = Group.Elements,
  name: UUID = UUID.randomUUID(),
  traverse: Boolean = true,
  data: EdgeData = EdgeData.empty,
  edgeId: UUID = UUID.randomUUID(),
)(implicit val assetType: AssetType[B])
