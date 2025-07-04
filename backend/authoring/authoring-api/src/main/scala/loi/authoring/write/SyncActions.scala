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

import enumeratum.{Enum, EnumEntry}
import loi.authoring.edge.{EdgeElem, Group}

import java.util.UUID

case class SyncActions(
  mergeNodes: Vector[MergeNode] = Vector.empty,
  mergeEdges: Vector[MergeEdge] = Vector.empty,
  spreadGroups: Vector[SpreadGroup] = Vector.empty,
  fastForwardNodes: Set[UUID] = Set.empty,
  fastForwardEdges: Set[NameAndSrcName] = Set.empty,
  claimNodes: Set[UUID] = Set.empty /* they made a new node new */,
  claimEdges: Set[NameAndSrcName] = Set.empty /* they made a new edge new */,
  omitNodes: Set[UUID] = Set.empty,
  omitEdges: Set[NameAndSrcName] = Set.empty,
  declineNodes: Vector[DeclineNode] = Vector.empty,
  declineEdges: Vector[DeclineEdge] = Vector.empty,
):

  def withMergeNode(merge: MergeNode): SyncActions            = copy(mergeNodes = mergeNodes.appended(merge))
  def withMergeEdge(merge: MergeEdge): SyncActions            = copy(mergeEdges = mergeEdges.appended(merge))
  def withSpread(spread: SpreadGroup): SyncActions            = copy(spreadGroups = spreadGroups.appended(spread))
  def withFastFowardNode(claim: UUID): SyncActions            = copy(fastForwardNodes = fastForwardNodes.incl(claim))
  def withFastForwardEdge(claim: NameAndSrcName): SyncActions = copy(fastForwardEdges = fastForwardEdges.incl(claim))
  def withClaimNode(claim: UUID): SyncActions                 = copy(claimNodes = claimNodes.incl(claim))
  def withClaimEdge(claim: NameAndSrcName): SyncActions       = copy(claimEdges = claimEdges.incl(claim))
  def withDeclineNode(decline: DeclineNode): SyncActions      = copy(declineNodes = declineNodes.appended(decline))
  def withDeclineEdge(decline: DeclineEdge): SyncActions      = copy(declineEdges = declineEdges.appended(decline))

  def rejectMerge(merge: MergeNode, why: DeclineReason): SyncActions = copy(
    mergeNodes = mergeNodes.filterNot(_.name == merge.name),
    declineNodes = declineNodes.appended(DeclineNode(merge.name, merge.theirId, why))
  )

  def rejectMerge(merge: MergeEdge, why: DeclineReason): SyncActions = copy(
    mergeEdges = mergeEdges.filterNot(_.name == merge.name),
    declineEdges = declineEdges.appended(DeclineEdge(merge.name, merge.srcName, merge.theirId, why))
  )

  /** The nodes that were updated in the Narrative sense (children are "attributes" of the source)
    */
  lazy val narrativelyUpdatedNodeNames: Set[UUID] =
    val updatedByMerge = mergeNodes.view.map(_.name)
    val updatedByFF    = fastForwardNodes.view
    val childUpdated   = mergeEdges.view.map(_.srcName)
    val childAdded     = claimEdges.view.map(_.srcName).filterNot(claimNodes.contains)
    val childRemoved   = omitEdges.view.map(_.srcName).filterNot(omitNodes.contains)

    updatedByMerge.concat(updatedByFF).concat(childUpdated).concat(childAdded).concat(childRemoved).toSet

    // spreadGroups are secondary actions, some primary action (MergeEdge or ClaimEdge) will provide the affected node
    // claimNodes are added nodes, not updated
    // when a claimEdge is a child of a claimNode then it hasn't updated anything, it is new
    // when an omitEdge is a child of an omitNode then it hasn't updated anything, its source was removed.
  end narrativelyUpdatedNodeNames
end SyncActions

object SyncActions:
  val empty: SyncActions = SyncActions()

case class MergeNode(name: UUID, ourId: Long, baseId: Long, theirId: Long)
case class MergeEdge(name: UUID, srcName: UUID, ourId: Long, baseId: Long, theirId: Long)
case class SpreadGroup(srcName: UUID, grp: Group, members: List[EdgeElem])
case class DeclineNode(name: UUID, theirId: Long, why: DeclineReason)
case class DeclineEdge(name: UUID, srcName: UUID, theirId: Long, why: DeclineReason)

// because Narrative Authoring speaks about edges in source node terms
case class NameAndSrcName(name: UUID, srcName: UUID)

sealed abstract class DeclineReason(val msg: String) extends EnumEntry

object DeclineReason extends Enum[DeclineReason]:
  case object WeExclude                           extends DeclineReason("our project excludes")
  case object DuplicatesTgt                       extends DeclineReason("our project already uses target")
  case object TooManyEdges                        extends DeclineReason(s"too many edges")
  case class MergeAbort(override val msg: String) extends DeclineReason(msg)

  override lazy val values: IndexedSeq[DeclineReason] = findValues
