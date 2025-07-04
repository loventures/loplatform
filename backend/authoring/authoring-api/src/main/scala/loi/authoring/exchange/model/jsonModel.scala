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

package loi.authoring.exchange.model

import com.fasterxml.jackson.databind.JsonNode
import loi.authoring.edge.Group
import loi.cp.asset.edge.EdgeData

import java.util.UUID

/** @param competencyIds
  *   the ids of `nodes` that are subject to the competency import policy. This policy does not import the `node`, but
  *   uses data in the `node` to find a node that already exists on the server. The node for an id listed in
  *   `competencyIds` is never imported.
  */
case class ExchangeManifest(
  version: String,
  nodes: Seq[NodeExchangeData],
  competencyIds: Set[String]
):
  lazy val size: Int = nodes.size

object ExchangeManifest:
  val currentVersion = "3"

  val empty = ExchangeManifest(currentVersion, Seq.empty, Set.empty)

/** @param attachment
  *   An optional filepath that'll be made into a blobref or an attachmentfinder row.
  */
case class NodeExchangeData(
  id: String,
  typeId: String,
  data: JsonNode,
  edges: Seq[EdgeExchangeData],
  attachment: Option[String]
)

case class EdgeExchangeData(
  group: Group,
  target: String,
  position: Long,
  traverse: Boolean,
  edgeId: UUID,
  edgeData: EdgeData,
  targetInWorkspace: Boolean = false,
)
