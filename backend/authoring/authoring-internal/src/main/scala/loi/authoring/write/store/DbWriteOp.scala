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

package loi.authoring.write.store

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import loi.asset.syntax.any.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.write.LayeredWriteService.ElementalNode
import loi.authoring.write.*
import loi.jackson.syntax.any.*
import scalaz.syntax.std.option.*

import java.lang
import java.util.UUID

/** Write ops shaped for DB storage. DB storage uses less useful but more resilient types for certain properties. These
  * ops will be stored long after nodes/edges are deleted. When they're read back we might not have a certain
  * AssetFactory or Group anymore.
  *
  * On the other hand, we may never try to deserialize them, but this DSL is needed even for serialization:
  *   - to put the type info into the value (the op property)
  *   - to replace AssetFactory with its typeId property
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "op")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[DbAddNode], name = "addNode"),
    new JsonSubTypes.Type(value = classOf[DbDeleteNode], name = "deleteNode"),
    new JsonSubTypes.Type(value = classOf[DbSetNodeData], name = "setNodeData"),
    new JsonSubTypes.Type(value = classOf[DbAddEdge], name = "addEdge"),
    new JsonSubTypes.Type(value = classOf[DbDeleteEdge], name = "deleteEdge"),
    new JsonSubTypes.Type(value = classOf[DbReplaceEdgeTarget], name = "replaceEdgeTarget"),
    new JsonSubTypes.Type(value = classOf[DbSetEdgeData], name = "setEdgeData"),
    new JsonSubTypes.Type(value = classOf[DbSetEdgeOrder], name = "setEdgeOrder"),
    new JsonSubTypes.Type(value = classOf[DbSetRootName], name = "setRootName"),
    new JsonSubTypes.Type(value = classOf[DbSetHomeName], name = "setHomeName"),
    new JsonSubTypes.Type(value = classOf[DbAddDependency], name = "addDependency"),
    new JsonSubTypes.Type(value = classOf[DbUpdateDependency], name = "updateDependency"),
  )
)
sealed trait DbWriteOp

case class DbAddNode(
  typeId: AssetTypeId,
  name: UUID,
  data: JsonNode
) extends DbWriteOp

object DbAddNode:
  def fromOp[A](op: AddNode[A]): DbAddNode          = DbAddNode(op.assetType.id, op.name, op.data.finatraEncoded)
  def fromOp[A](op: ValidatedAddNode[A]): DbAddNode = DbAddNode(op.assetType.id, op.name, op.data.finatraEncoded)

case class DbDeleteNode(
  name: UUID,
  typeId: AssetTypeId,
  title: String
) extends DbWriteOp

object DbDeleteNode:
  def fromOp[A](op: DeleteNode, prev: ElementalNode[A]): DbDeleteNode =
    DbDeleteNode(op.name, prev.assetType.id, prev.data.title.getOrElse(""))

case class DbSetNodeData(
  typeId: AssetTypeId,
  name: UUID,
  data: JsonNode
) extends DbWriteOp

object DbSetNodeData:
  def fromOp[A](op: SetNodeData[A]): DbSetNodeData = DbSetNodeData(op.assetType.id, op.name, op.data.finatraEncoded)

case class DbAddEdge(
  name: UUID,
  sourceName: UUID,
  targetName: UUID,
  group: String,
  // None before May 2023
  @JsonDeserialize(contentAs = classOf[lang.Long])
  position: Option[Long],
  traverse: Boolean,
  data: JsonNode,
  edgeId: UUID,
) extends DbWriteOp

object DbAddEdge:
  def fromOp(op: AddEdge, pos: Int): DbAddEdge = DbAddEdge(
    op.name,
    op.sourceName,
    op.targetName,
    op.group.entryName,
    pos.toLong.some,
    op.traverse,
    op.data.finatraEncoded,
    op.edgeId
  )

  def fromOp(op: ValidatedAddEdge): DbAddEdge = DbAddEdge(
    op.name,
    op.sourceName,
    op.targetName,
    op.group.entryName,
    op.position.some,
    op.traverse,
    op.data.finatraEncoded,
    op.edgeId
  )
end DbAddEdge

case class DbDeleteEdge(
  name: UUID,
  sourceName: Option[UUID], // None before Feb 2023
  group: Option[Group],     // None before Feb 2023
  targetName: Option[UUID], // None before Feb 2023
  safeDeleteTarget: Boolean // always false from Sept 2022
) extends DbWriteOp

object DbDeleteEdge:
  def fromOp(op: DeleteEdge, src: UUID, grp: Group, tgt: UUID): DbDeleteEdge =
    DbDeleteEdge(op.name, src.some, grp.some, tgt.some, safeDeleteTarget = false)

// Op deleted in Dec 2022
case class DbReplaceEdgeTarget(
  name: UUID,
  targetName: UUID,
  safeDeleteTarget: Boolean // always false from Sept 2022
) extends DbWriteOp

case class DbSetEdgeData(
  name: UUID,
  data: JsonNode
) extends DbWriteOp

object DbSetEdgeData:
  def fromOp(op: SetEdgeData): DbSetEdgeData = DbSetEdgeData(op.name, op.data.finatraEncoded)

case class DbSetEdgeOrder(
  sourceName: UUID,
  group: String,
  ordering: Seq[UUID]
) extends DbWriteOp

object DbSetEdgeOrder:
  def fromOp(op: SetEdgeOrder): DbSetEdgeOrder = DbSetEdgeOrder(
    op.sourceName,
    op.group.entryName,
    op.ordering
  )

case class DbSetRootName(name: UUID) extends DbWriteOp

case class DbSetHomeName(name: UUID) extends DbWriteOp

case class DbAddDependency(projectId: Long, projectTitle: String, projectCode: Option[String]) extends DbWriteOp

// Page History wants to say things like:
//   * "Stephen edited this lesson".
//   * "Stephen added 'child title' to this lesson".
//   * "Stephen removed 'child title' from this lesson".
// But, synchronization can do all three of those multiple times in a single revision.
// So just say "Stephen synchronized this lesson with project Blue".
// And include a link to the report.
// To avoid Page History needing to decode every report, the affected nodes are repeated on this event.
// "Affected" nodes are
// * mergeNode
// * the source of a mergeEdge
// * the source of a claimEdge the source is not a claimNode
// * the source of an omitEdge if the source is not an omitNode
case class DbUpdateDependency(
  projectId: Long,
  projectTitle: String,
  projectCode: Option[String],
  narrativelyUpdatedNodeNames: Set[UUID],
  narrativelyAddedNodeNames: Set[UUID],
  narrativelyRemovedNodeNames: Set[UUID],
  reportId: Long
) extends DbWriteOp

object DbWriteOp:

  private val om = JacksonUtils.getFinatraMapper

  def fromValidatedOp(op: ValidatedWriteOp): DbWriteOp =
    op match
      case op: ValidatedAddNode[?]     => DbAddNode(op.assetType.id, op.name, om.valueToTree(op.data))
      case op: ValidatedSetNodeData[?] => DbSetNodeData(op.assetType.id, op.name, om.valueToTree(op.data))
      case op: ValidatedAddEdge        =>
        DbAddEdge(
          op.name,
          op.sourceName,
          op.targetName,
          op.group.entryName,
          op.position.some,
          op.traverse,
          om.valueToTree(op.data),
          op.edgeId,
        )
      case op: ValidatedDeleteEdge     =>
        DbDeleteEdge(
          op.name,
          op.existingEdge.source.info.name.some,
          op.existingEdge.group.some,
          op.existingEdge.target.info.name.some,
          safeDeleteTarget = false
        )
      case op: ValidatedSetEdgeOrder   => DbSetEdgeOrder(op.sourceName, op.group.entryName, op.ordering)
      case op: ValidatedSetEdgeData    => DbSetEdgeData(op.name, om.valueToTree(op.data))
      case op: ValidatedSetRootName    => DbSetRootName(op.name)
      case op: ValidatedSetHomeName    => DbSetHomeName(op.name) // up to 6th instantiation of a UUID wrapper
end DbWriteOp
