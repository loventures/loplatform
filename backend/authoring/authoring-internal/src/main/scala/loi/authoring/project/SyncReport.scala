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

package loi.authoring.project

import argonaut.{CodecJson, DecodeJson, EncodeJson}
import cats.syntax.option.*
import enumeratum.EnumEntry.Uncapitalised
import enumeratum.{ArgonautEnum, Enum, EnumEntry}
import kantan.csv.{CellEncoder, HeaderEncoder}
import loi.authoring.write.*
import scaloi.json.ArgoExtras

import java.util.UUID

// this is just SyncActions with higher entropy.
// this class is json-codeced in authoringcommit and serialized to CSV with Katan, tread lightly
case class SyncReport(rows: List[SyncReport.Row])

object SyncReport:

  def fromSyncActions(actions: SyncActions): SyncReport =

    val staircase = actions.mergeNodes.view.map(Row.fromMergeNode) ++
      actions.mergeEdges.view.map(Row.fromMergeEdge) ++
      actions.spreadGroups.view.map(Row.fromSpreadGroup) ++
      actions.fastForwardNodes.view.map(Row.fromFastForwardNode) ++
      actions.fastForwardEdges.view.map(Row.fromFastForwardEdge) ++
      actions.claimNodes.view.map(Row.fromClaimNode) ++
      actions.claimEdges.view.map(Row.fromClaimEdge) ++
      actions.omitNodes.view.map(Row.fromOmitNode) ++
      actions.omitEdges.view.map(Row.fromOmitEdge) ++
      actions.declineNodes.view.map(Row.fromDeclineNode) ++
      actions.declineEdges.view.map(Row.fromDeclineEdge)

    SyncReport(staircase.toList)
  end fromSyncActions

  implicit val encodeJsonForSyncReport: EncodeJson[SyncReport] = EncodeJson.of[List[SyncReport.Row]].contramap(_.rows)
  implicit val decodeJsonForSyncReport: DecodeJson[SyncReport] =
    DecodeJson.of[List[SyncReport.Row]].map(SyncReport.apply)

  // there can be multiple rows one nodeName (i.e. a Merge and a SpreadGroup)
  case class Row(
    nodeName: UUID,
    action: Action,
    childEdgeName: Option[UUID] = None,
    ourId: Option[Long] = None,
    baseId: Option[Long] = None,
    theirId: Option[Long] = None,
    group: Option[String] = None,
    declineReason: Option[String] = None
  )

  object Row:

    def fromMergeNode(a: MergeNode): Row =
      Row(a.name, Action.Merge, None, a.ourId.some, a.baseId.some, a.theirId.some)

    def fromMergeEdge(a: MergeEdge): Row =
      Row(a.srcName, Action.Merge, a.name.some, a.ourId.some, a.baseId.some, a.theirId.some)

    def fromDeclineNode(a: DeclineNode): Row =
      Row(a.name, Action.Decline, theirId = a.theirId.some, declineReason = a.why.msg.some)

    def fromDeclineEdge(a: DeclineEdge): Row =
      Row(a.srcName, Action.Decline, a.name.some, theirId = a.theirId.some, declineReason = a.why.msg.some)

    def fromSpreadGroup(a: SpreadGroup): Row        = Row(a.srcName, Action.SpreadGroup, group = a.grp.entryName.some)
    def fromFastForwardNode(a: UUID): Row           = Row(a, Action.FastForward)
    def fromFastForwardEdge(a: NameAndSrcName): Row = Row(a.srcName, Action.FastForward, a.name.some)
    def fromClaimNode(a: UUID): Row                 = Row(a, Action.Claim)
    def fromClaimEdge(a: NameAndSrcName): Row       = Row(a.srcName, Action.Claim, a.name.some)
    def fromOmitNode(a: UUID): Row                  = Row(a, Action.Omit)
    def fromOmitEdge(a: NameAndSrcName): Row        = Row(a.srcName, Action.Omit, a.name.some)

    implicit val codecJsonForRow: CodecJson[Row] = CodecJson.casecodec8(Row.apply, ArgoExtras.unapply)(
      "nodeName",
      "action",
      "childEdgeName",
      "ourId",
      "baseId",
      "theirId",
      "group",
      "declineReason"
    )

    implicit val headerEncoderForRow: HeaderEncoder[Row] = HeaderEncoder.caseEncoder(
      "Node Name",
      "Action",
      "Child Edge Name",
      "Our ID",
      "Base ID",
      "Their ID",
      "Group",
      "Decline Reason"
    )(ArgoExtras.unapply)
  end Row

  sealed trait Action extends EnumEntry with Uncapitalised

  object Action extends Enum[Action] with ArgonautEnum[Action]:
    case object Merge       extends Action
    case object FastForward extends Action
    case object Claim       extends Action
    case object Omit        extends Action
    case object Decline     extends Action
    case object SpreadGroup extends Action

    override lazy val values: IndexedSeq[Action] = findValues

    implicit val cellEncoderForAction: CellEncoder[Action] = CellEncoder.from(_.entryName)
  end Action
end SyncReport
