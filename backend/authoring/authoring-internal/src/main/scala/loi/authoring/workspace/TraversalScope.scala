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

/** A subset of nodeIds of a workspace. The subset is used to traverse one or more graphs in the workspace, instead of
  * the entire workspace. This is used by export and deep copy, as they are constrained to one graph (maybe more in the
  * future).
  *
  * A true boolean value for a nodeId means the nodeId is for a competency-related node. False means it is ordinary
  * content. This has meaning to export and deep copy.
  */
case class TraversalScope(nodeIds: Map[Long, Boolean]):

  def addAll(edgeInfos: Iterable[EdgeInfo]): TraversalScope =
    edgeInfos.foldLeft(this)((acc, info) => acc.add(info))

  def add(edgeInfo: EdgeInfo): TraversalScope =

    val tgtId = edgeInfo.targetId

    if edgeInfo.traverse then
      // false means the id is of a node that should be created
      copy(nodeIds = nodeIds.updated(tgtId, false))
    else if !nodeIds.contains(tgtId) then
      // we check membership because if there is any edge to tgtId whose traverse value
      // is true then that situation overrides any other edge whose traverse value
      // is false. AKA the membership check is us OR-ing the traverse values of all edges
      // to tgtId
      copy(nodeIds = nodeIds.updated(tgtId, true))
    else this
    end if
  end add
end TraversalScope

object TraversalScope:

  val empty = TraversalScope(Map.empty)

  def build(
    workspace: ReadWorkspace,
    startingNodeIds: Set[Long],
    includeNonTraverse: Boolean = false
  ): TraversalScope =

    def loop(scope: TraversalScope, nodeIds: Set[Long]): TraversalScope =
      val (nextScope, nextIds) = nodeIds.foldLeft((scope, Set.empty[Long]))({ case ((scopeAcc, nextIdsAcc), nodeId) =>
        val outEdges         = workspace.outEdgeInfos(nodeId)
        val nextScope        = scopeAcc.addAll(outEdges)
        val filteredOutEdges = if includeNonTraverse then outEdges else outEdges.filter(_.traverse)
        val nextIds          = nextIdsAcc ++ filteredOutEdges.map(_.targetId).toSet
        (nextScope, nextIds)
      })

      if nextIds.isEmpty then nextScope
      else loop(nextScope, nextIds)
    end loop

    val startScope = startingNodeIds.foldLeft(TraversalScope.empty)((acc, nodeId) =>
      acc.copy(nodeIds = acc.nodeIds.updated(nodeId, false))
    )
    loop(startScope, startingNodeIds)
  end build
end TraversalScope
