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

package loi.authoring

import loi.authoring.edge.{EdgeElem, Group}
import loi.authoring.workspace.EdgeInfo

import java.util.UUID

case class GraphData2(
  nodes: Set[NodeInfo2],
  edges: Set[EdgeElem]
):

  private lazy val nodeNames: Set[UUID] = nodes.map(_.name)

  /** A boundary edge is an edge whose target is not in `nodes`. It is on the boundary of the graph region that this
    * object is representing. The source and the edge itself are in `nodes` and `edges` respectively.
    */
  lazy val boundaryEdgeNames: Set[UUID] =
    edges.filterNot(e => nodeNames.contains(e.tgtName)).map(_.name)
end GraphData2

case class NodeInfo2(name: UUID, id: Long)

case class EdgeInfo2(
  id: Long,
  name: UUID,
  source: NodeInfo2,
  target: NodeInfo2,
  edgeId: UUID,
  group: Group,
  position: Long,
  traverse: Boolean,
):
  lazy val toEdgeInfo = EdgeInfo(id, name, source.id, target.id, edgeId, group, position, traverse)
end EdgeInfo2

object EdgeInfo2:

  def apply(e: EdgeInfo, source: NodeInfo2, target: NodeInfo2): EdgeInfo2 =
    EdgeInfo2(
      e.id,
      e.name,
      source,
      target,
      e.edgeId,
      e.group,
      e.position,
      e.traverse,
    )
end EdgeInfo2
