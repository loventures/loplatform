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

package loi.authoring.edge

import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.cp.asset.edge.EdgeData

import java.sql.Timestamp
import java.util.UUID

/** An edge between two asset nodes
  *
  * @param id
  *   the id
  * @param name
  *   the edge name is the same value for all versions of the edge, like a filename in git. The edge name is unique in a
  *   workspace.
  * @param edgeId
  *   the edge id is used by the source node of the edge to refer to the edge in a data value. The edge id is the same
  *   value for all versions of the edge. The edge id is unique amongst the edges of the source.
  * @param source
  *   the source node of this edge
  * @param target
  *   the target node of this edge
  * @param group
  *   the group of this edge. This value can be used to give meaning to a set of targets. The same target can appear in
  *   more than one group, but not more than once in the same group.
  * @param position
  *   a 0-indexed value that orders the edges. Amongst a `group` of edges, the position must be unique and gap-less and
  *   the range must start at 0.
  * @param traverse
  *   whether or not the edge is traversed in clone or import. This value has special and slightly different meaning to
  *   clone and import.
  * @param data
  *   data about this edge
  */
final case class AssetEdge[S, T](
  id: Long,
  name: UUID,
  edgeId: UUID,
  source: Asset[S],
  target: Asset[T],
  group: Group,
  position: Long,
  traverse: Boolean,
  data: EdgeData,
  created: Timestamp,
  modified: Timestamp
):

  /** Obtains this edge with source of type Asset[B] if source is a B
    */
  def filterSrc[B](implicit bType: AssetType[B]): Option[AssetEdge[B, T]] =
    source.filter[B].map(_ => this.asInstanceOf[AssetEdge[B, T]])

  /** Obtains this edge with target of type Asset[B] if target is a B
    */
  def filterTgt[B](implicit bType: AssetType[B]): Option[AssetEdge[S, B]] =
    target.filter[B].map(_ => this.asInstanceOf[AssetEdge[S, B]])
end AssetEdge

object AssetEdge:

  /* some ugly type aliases so that existential types don't make our signatures as ugly */

  /** An asset edge from an asset of type `S` */
  type AnyFrom[S] = AssetEdge[S, ?]

  /** An asset edge to an asset of type `T` */
  type AnyTo[T] = AssetEdge[?, T]

  /** An asset edge with no information about the asset types */
  type Any = AssetEdge[?, ?]

  final val Gap = 1 << 14 // We space positions by this (16384) to avoid excessive edge position rewrites
end AssetEdge
