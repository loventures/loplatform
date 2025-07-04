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
import scalaz.std.list.*
import scalaz.syntax.std.boolean.*
import scaloi.data.ListTree
import scaloi.syntax.collection.*

import java.util.UUID

case class TraversedGraph(
  sources: Seq[Asset[?]],
  edges: Seq[AssetEdge[?, ?]]
):

  private lazy val outEdges0 = edges.groupBy(_.source.info.id)
  private lazy val inEdges0  = edges.groupBy(_.target.info.id)

  /** out edges from source, ordered by group, position, then target-id ascending (the target id ordering is for
    * stability if you are paging)
    */
  def outEdges(source: Asset[?]): Seq[AssetEdge[?, ?]] =
    outEdges0.getOrElse(source.info.id, Nil).sortBy(e => (e.group.entryName, e.position, e.target.info.id))

  /** in edges to target, ordered by source id then group ascending
    */
  def inEdges(target: Asset[?]): Seq[AssetEdge[?, ?]] =
    inEdges0.getOrElse(target.info.id, Nil).sortBy(e => (e.source.info.id, e.group.entryName))

  lazy val nodes: Seq[Asset[?]] =
    edges.foldLeft(sources.toList)((assets, edge) => edge.source :: edge.target :: assets).distinct

  lazy val nodesByName: Map[UUID, Asset[?]] = nodes.groupUniqBy(_.info.name)

  def node[A: AssetType](name: UUID): Option[Asset[A]] =
    nodesByName.get(name).flatMap(_.filter[A])

  /** Ordered edges from [source] in group [group]. */
  def outEdgesInGroup(source: Asset[?], group: Group, groupTail: Group*): Seq[AssetEdge[?, ?]] =
    val groups = Set(group) ++ groupTail
    outEdges(source).filter(edge => groups.contains(edge.group))

  /** Ordered targets with edges from [source] in group [group]. */
  def targetsInGroup(source: Asset[?], group: Group, groupTail: Group*): Seq[Asset[?]] =
    outEdgesInGroup(source, group, groupTail*).map(_.target)

  /** Ordered targets with edges from [source] in group [group] of type [A]. */
  def targetsInGroupOfType[A: AssetType](source: Asset[?], group: Group, groupTail: Group*): Seq[Asset[A]] =
    outEdgesInGroup(source, group, groupTail*).flatMap(_.filterTgt[A]).map(_.target)

  /** The out-trees of `source` in this graph, sorted by group then position. An out-tree is an out-edge and its entire
    * sub-graph, converted into a tree. The elements of the trees are edges. If it helps, think of each edge as "a
    * target node and a bunch of edgy properties"
    *
    * Tree Conversion Conversion is a depth-first traversal of the graph in the order of edge group then position.
    * Cycles are removed by ignoring any edge that has already been visited in the traversal path. Indegree is lowered
    * to 1 by repeating nodes that are reused, such that each duplicate has one in-edge. This results in duplicated
    * descendants too. Because of this duplication, edge names are no longer unique identifiers. The path of edge names
    * is the only unique id.
    */
  def outTrees(source: Asset[?]): List[ListTree[AssetEdge[?, ?]]] =

    def loop(edges: List[AssetEdge[?, ?]], visited: Set[Long]): List[ListTree[AssetEdge[?, ?]]] =
      val freshEdges = edges.filterNot(edge => visited.contains(edge.id))
      freshEdges.map(e => ListTree.Node(e, e.traverse ?? loop(outEdges(e.target).toList, visited + e.id)))

    loop(outEdges(source).toList, Set.empty)
end TraversedGraph
