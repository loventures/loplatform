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

package loi.authoring.workspace

import loi.authoring.edge.{AssetEdge, EdgeAttrs, Group}

import java.util.UUID

/** @param id
  *   unique value amongst all edges.
  * @param name
  *   unique value amongst all edges in a workspace.
  * @param edgeId
  *   unique value amongst all out-edges of a node. When a node is duplicated, the edgeId is cloned. Whereas `name` is
  *   generated anew. This mechanism allows source nodes to use edgeIds to refer to edges such that a duplication allows
  *   us to copy the source data verbatim.
  */
case class EdgeInfo(
  id: Long,
  name: UUID,
  sourceId: Long,
  targetId: Long,
  edgeId: UUID,
  group: Group,
  position: Long,
  traverse: Boolean,
):
  lazy val vertexIds: Seq[Long] = Seq(sourceId, targetId)
end EdgeInfo

object EdgeInfo:

  def apply(edge: AssetEdge[?, ?]): EdgeInfo =
    EdgeInfo(
      edge.id,
      edge.name,
      edge.source.info.id,
      edge.target.info.id,
      edge.edgeId,
      edge.group,
      edge.position,
      edge.traverse,
    )

  def apply(id: Long, attrs: EdgeAttrs, srcId: Long, tgtId: Long): EdgeInfo =
    EdgeInfo(
      id,
      attrs.name,
      srcId,
      tgtId,
      attrs.localId,
      attrs.group,
      attrs.position,
      attrs.traverse,
    )
end EdgeInfo
