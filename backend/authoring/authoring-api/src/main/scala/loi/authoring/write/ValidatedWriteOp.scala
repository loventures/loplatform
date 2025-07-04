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

import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.edge.{AssetEdge, Group}
import loi.cp.asset.edge.EdgeData

import java.util.UUID

/** Meant to restrict callers to StageAndWriteService to those who've already validated their WriteOps via
  * WriteValidationService. Otherwise, they are identical to WriteOps.
  */
sealed trait ValidatedWriteOp

case class ValidatedAddNode[A](
  name: UUID,
  data: A,
)(implicit val assetType: AssetType[A])
    extends ValidatedWriteOp

case class ValidatedSetNodeData[A](
  name: UUID,
  data: A,
  existingNode: Asset[?] // of course its supposed to be of type `A`
)(implicit val assetType: AssetType[A])
    extends ValidatedWriteOp

/** @param addDurableEdge
  *   true if `durableEdgeKey` does not already exist in the DB, false otherwise
  */
case class ValidatedAddEdge(
  name: UUID,
  sourceName: UUID,
  targetName: UUID,
  group: Group,
  position: Long,
  traverse: Boolean,
  data: EdgeData,
  edgeId: UUID,
  addDurableEdge: Boolean,
) extends ValidatedWriteOp:
  lazy val vertexNames: Seq[UUID] = Seq(sourceName, targetName)
end ValidatedAddEdge

case class ValidatedDeleteEdge(
  name: UUID,
  existingEdge: AssetEdge.Any
) extends ValidatedWriteOp

case class ValidatedSetEdgeData(
  name: UUID,
  data: EdgeData,
  existingEdge: AssetEdge.Any,
  // if this updates a commit, rewrite all remote edges
  rewriteRemoteEdges: List[RewriteRemoteEdge],
) extends ValidatedWriteOp

final case class RewriteRemoteEdge(existingEdge: AssetEdge.Any)

case class ValidatedSetEdgeOrder(
  sourceName: UUID,
  group: Group,
  ordering: Seq[UUID],
  // caution: add edges have their position, these are only existing edges
  setEdgePositions: Seq[SetEdgePosition]
) extends ValidatedWriteOp

case class ValidatedSetRootName(name: UUID) extends ValidatedWriteOp

case class ValidatedSetHomeName(name: UUID) extends ValidatedWriteOp

case class SetEdgePosition(existingEdge: AssetEdge.Any, position: Long)
