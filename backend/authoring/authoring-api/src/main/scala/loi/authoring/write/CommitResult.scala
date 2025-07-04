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

import loi.authoring.workspace.AttachedReadWorkspace

import java.util.UUID

/** The result of committing write operations.
  *
  * In Laird, newNodes == modifiedNodes and likewise for edges, b/c there is no ancestor-copy
  *
  * @param ws
  *   the workspace for the new commit, contains the commit
  * @param newNodes
  *   added/modified/ancestor-copied nodes, aka new rows in assetnode
  * @param newEdges
  *   added/modified/ancestor-copied edges, aka new rows in assetedge
  * @param modifiedNodes
  *   added/modified nodes, any node where human modify time == create time
  * @param modifiedEdges
  *   added/modified edges, any edge where human modify time == create time
  * @param deletedEdges
  *   edges omitted from the new commit
  * @param recoveredEdges
  *   added edge names indexed by requested name
  * @param demotedNodes
  *   existing nodes that are no longer root in the new commit
  * @param promotedNodes
  *   existing nodes that are now root in the new commit
  * @param catastrophic
  *   this commit result has been rendered garbage by multiverse changes
  * @param squashed
  *   this commit was squashed onto the prior commit
  */
case class CommitResult[+A <: AttachedReadWorkspace](
  ws: A,
  newNodes: Map[UUID, Long],
  newEdges: Map[UUID, Long],
  modifiedNodes: Map[UUID, Long],
  modifiedEdges: Map[UUID, Long],
  deletedNodes: Set[UUID],
  deletedEdges: Set[UUID],
  recoveredEdges: Map[UUID, UUID],
  demotedNodes: Set[UUID],
  promotedNodes: Set[UUID],
  catastrophic: Boolean,
  squashed: Boolean,
)

object CommitResult:
  def empty[A <: AttachedReadWorkspace](ws: A): CommitResult[A] =
    CommitResult(
      ws,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Set.empty,
      Set.empty,
      Map.empty,
      Set.empty,
      Set.empty,
      catastrophic = false,
      squashed = true,
    )
end CommitResult
