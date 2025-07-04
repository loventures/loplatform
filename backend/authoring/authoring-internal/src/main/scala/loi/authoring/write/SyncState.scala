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

import cats.data.State
import loi.authoring.edge.{EdgeElem, Group}
import loi.authoring.project.FuzzyLayerBase
import loi.authoring.workspace.ProjectWorkspace
import loi.authoring.write.LayeredWriteService.*
import loi.authoring.{EdgeId, NodeId, ProjectId}

import java.util.UUID

// ValState builds InsertState by folding an unpredictable, chaotic, staccato, List[WriteOp]
// SyncState builds InsertState by an ordered theirWs graph traversal. This permits some shortcuts.
// since we know that we only visit nodes and their children at most once.
case class SyncState(
  insertState: InsertState,
  actions: SyncActions,
  visitedNodeNames: Set[UUID],
  nodeReqs: Set[NodeId],
  edgeReqs: Set[EdgeId],
):

  def withMergeNode(name: UUID, ourId: Long, baseId: Long, theirId: Long): SyncState = copy(
    actions = actions.withMergeNode(MergeNode(name, ourId, baseId, theirId)),
    // to do a 3-way merge, we need the authoringnode.data for each way
    nodeReqs = nodeReqs.incl(ourId).incl(baseId).incl(theirId),
    visitedNodeNames = visitedNodeNames.incl(name)
  )

  def withFastForwardNode(name: UUID, theirId: Long, theirProjectId: ProjectId): SyncState = copy(
    insertState = insertState.withPendingNode(name, ClaimNode(theirProjectId, theirId)),
    actions = actions.withFastFowardNode(name),
    visitedNodeNames = visitedNodeNames.incl(name)
  )

  def withClaimNode(name: UUID, theirId: Long, theirProjectId: ProjectId): SyncState = copy(
    insertState = insertState.withPendingNode(name, ClaimNode(theirProjectId, theirId)),
    actions = actions.withClaimNode(name),
    visitedNodeNames = visitedNodeNames.incl(name)
  )

  def withDeclineNode(decline: DeclineNode): SyncState = copy(
    actions = actions.withDeclineNode(decline),
    visitedNodeNames = visitedNodeNames.incl(decline.name)
  )

  def withMergeEdge(name: UUID, ours: EdgeElem, baseId: Long, theirEdge: EdgeElem): SyncState = copy(
    actions = actions.withMergeEdge(MergeEdge(name, theirEdge.srcName, ours.id, baseId, theirEdge.id)),
    // to do a 3-way merge, we need the authoringedge.data for each way
    edgeReqs = edgeReqs.incl(ours.id).incl(baseId).incl(theirEdge.id)
  )

  def withFastForwardEdge(theirEdge: EdgeElem, theirProjectId: ProjectId, createDurableEdge: Boolean): SyncState = copy(
    insertState = insertState.withPendingEdge(theirEdge.name, ClaimEdge(theirProjectId, theirEdge, createDurableEdge)),
    actions = actions.withFastForwardEdge(NameAndSrcName(theirEdge.name, theirEdge.srcName))
  )

  def withClaimEdge(theirEdge: EdgeElem, theirProjectId: ProjectId, createDurableEdge: Boolean): SyncState = copy(
    insertState = insertState.withPendingEdge(theirEdge.name, ClaimEdge(theirProjectId, theirEdge, createDurableEdge)),
    actions = actions.withClaimEdge(NameAndSrcName(theirEdge.name, theirEdge.srcName))
  )

  def withDeclineEdge(decline: DeclineEdge): SyncState = copy(actions = actions.withDeclineEdge(decline))

  def acceptMerge(node: InsertNode[?]): SyncState            = copy(insertState = insertState.withPendingNode(node.name, node))
  def acceptMerge(edge: InsertEdge, dur: Boolean): SyncState = copy(insertState = insertState.withMergedEdge(edge, dur))

  def rejectMerge(merge: MergeNode, why: DeclineReason): SyncState = copy(actions = actions.rejectMerge(merge, why))
  def rejectMerge(merge: MergeEdge, why: DeclineReason): SyncState = copy(actions = actions.rejectMerge(merge, why))

  def withSpreadGroup(srcName: UUID, grp: Group, members: List[EdgeElem]): SyncState = copy(
    actions = actions.withSpread(SpreadGroup(srcName, grp, members)),
    edgeReqs = edgeReqs ++ members.view.map(_.id)
  )

  /** Gets the "claim-traversable" target name of `edge`
    */
  def getClaimTraversableTgtName(edge: EdgeElem): Option[UUID] =
    LayeredWriteService.claimTraversableTgtName(edge, visitedNodeNames)
end SyncState

object SyncState:

  /** @param base
    *   name-ids of the ancestor of `theirWs` that `ourWs` is currently using (the Dep.commitId)
    */
  def initial(
    ourWs: LayeredWriteWorkspace,
    base: FuzzyLayerBase,
    theirWs: ProjectWorkspace
  ): SyncState =

    // Elements that no longer exist in their project are taken away from our project, even if we customized them.
    // This is different than us excluding one of their elements. Us excluding an element is a customization - a hide.
    // The customization (or hide) (if present) is wiped away here along with the element.
    val omitNodeNames = base.nodeIds.keySet.removedAll(theirWs.nodeNames)
    val omitEdgeNames = base.fuzzyEdgeIds.keySet.removedAll(theirWs.fuzzyEdgeNames)
    val nextBase      = theirWs.toFuzzyLayerBase

    // flatMap could drop some omitEdgeNames though /shrug
    val omitEdges = omitEdgeNames flatMap { omitEdgeName =>
      ourWs.getFuzzyEdgeAttrs(omitEdgeName).map(attrs => NameAndSrcName(omitEdgeName, attrs.srcName))
    }

    SyncState(
      InsertState.forSync(ourWs, theirWs.project.id, theirWs.commitId, nextBase, omitNodeNames, omitEdgeNames),
      SyncActions(omitNodes = omitNodeNames, omitEdges = omitEdges),
      visitedNodeNames = omitNodeNames,
      Set.empty,
      Set.empty,
    )
  end initial

  type SyncStateT[A] = State[SyncState, A]

  // duplicates `object State` but sets the type parameters for type inference's sake
  object SyncStateT:
    def apply[A](f: SyncState => (SyncState, A)): SyncStateT[A] = State.apply(f)
    def modify(f: SyncState => SyncState): SyncStateT[Unit]     = State.modify(f)
    def inspect[A](f: SyncState => A): SyncStateT[A]            = State.inspect(f)
    def pure[A](a: A): SyncStateT[A]                            = State.pure(a)
end SyncState
