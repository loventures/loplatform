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

import cats.syntax.option.*
import scaloi.syntax.int.*

import java.util.UUID

case class SyncResult(
  cr: CommitResult[LayeredWriteWorkspace],
  actions: SyncActions
):

  def ws: LayeredWriteWorkspace = cr.ws

  // takes the Narrative POV that modifications to edges are modifications to the source node
  lazy val modifiedNodes: Set[UUID] =
    cr.modifiedNodes.keySet ++ (cr.deletedEdges ++ cr.modifiedEdges.keys).view
      .flatMap(cr.ws.getEdgeAttrs)
      .map(_.srcName)

  // reminder that it is not possible to delete a node. But they could have excluded the name, making it "deleted" to us.
  lazy val deletedNodes: Set[UUID] = actions.omitNodes

  lazy val summary: String =

    val updated     = if modifiedNodes.nonEmpty then s"updated ${modifiedNodes.size.labelled("asset")}".some else None
    val deleted     = if deletedNodes.nonEmpty then s"deleted ${deletedNodes.size.labelled("asset")}".some else None
    val numDeclined = actions.declineNodes.size + actions.declineEdges.size
    val declined    = if numDeclined > 0 then s"declined ${numDeclined.labelled("change")}".some else None

    val texts = List(updated, deleted, declined).flatten
    if texts.isEmpty then "nothing changed"
    else texts.mkString("; ")
  end summary
end SyncResult

object SyncResult:

  def empty(ws: LayeredWriteWorkspace): SyncResult = SyncResult(
    CommitResult.empty(ws),
    SyncActions.empty
  )
