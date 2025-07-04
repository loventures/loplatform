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

package loi.authoring.read.web

import loi.authoring.asset.Asset
import loi.authoring.edge.{AssetEdge, Group, TraversedGraph}
import loi.authoring.workspace.ReadWorkspace
import loi.cp.asset.edge.EdgeData
import scaloi.syntax.collection.*
import scaloi.syntax.localDateTime.*

import java.util.{Date, UUID}

private[web] case class StructureResponse(
  commit: StructureCommit,
  edges: Map[UUID, EdgeStructureResponse],
  nodes: Map[UUID, Asset[?]],
  branchId: Option[Long],
  rootNodeName: Option[UUID],
  homeNodeName: Option[UUID],
  branchCommits: Option[Map[Long, Long]],
  assetBranches: Option[Map[UUID, Long]],
  customizedAssets: Option[Set[UUID]], // Remote assets locally modified
  layered: Boolean,
  delta: Boolean,
)

private[web] object StructureResponse:
  def apply(
    ws: ReadWorkspace,
    graph: TraversedGraph,
    branchId: Option[Long] = None,
    rootName: Option[UUID] = None,
    homeName: Option[UUID] = None,
    branchCommits: Option[Map[Long, Long]] = None,
    assetBranches: Option[Map[UUID, Long]] = None,
    customizedAssets: Option[Set[UUID]] = None,
    delta: Boolean = false,
  ): StructureResponse =
    val edges = graph.edges.groupMapUniq(_.name)(EdgeStructureResponse.apply)
    val nodes = graph.nodesByName
    StructureResponse(
      StructureCommit(ws.commitId, ws.created.asDate),
      edges,
      nodes,
      branchId,
      rootName,
      homeName,
      branchCommits,
      assetBranches,
      customizedAssets,
      ws.layered,
      delta,
    )
  end apply
end StructureResponse

private[web] final case class StructureCommit(
  id: Long,
  created: Date,
)

private[web] case class EdgeStructureResponse(
  id: Long,
  name: UUID,
  sourceName: UUID,
  targetName: UUID,
  data: EdgeData,
  position: Long,
  group: Group,
  edgeId: UUID,
  created: Date,
  modified: Date,
  traverse: Boolean,
)

private[web] object EdgeStructureResponse:
  def apply(edge: AssetEdge[?, ?]): EdgeStructureResponse =
    EdgeStructureResponse(
      edge.id,
      edge.name,
      edge.source.info.name,
      edge.target.info.name,
      edge.data,
      edge.position,
      edge.group,
      edge.edgeId,
      edge.created,
      edge.modified,
      edge.traverse,
    )
end EdgeStructureResponse
