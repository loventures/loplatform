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

package loi.authoring.write.web

import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.asset.Asset
import loi.authoring.commit.Commit
import loi.authoring.edge.AssetEdge
import loi.authoring.workspace.AttachedReadWorkspace
import scaloi.syntax.localDateTime.*

import java.util.{Date, UUID}

case class WriteResponse(
  newNodes: Map[String, UUID], // frontend's id + backend's name
  newEdges: Map[String, UUID], // frontend's id + backend's name
  deletedEdges: Set[UUID],
  nodes: Map[UUID, Asset[?]],
  edges: Map[UUID, AssetEdge[?, ?]],
  users: Map[Long, UserDTO],
  customizedAssets: Set[UUID],
  priorCommit: WriteCommit,
  commit: WriteCommit,
  squashed: Boolean,
)

object WriteResponse:
  def apply(
    nodes: Seq[Asset[?]],
    edges: Seq[AssetEdge[?, ?]],
    deletedEdges: Set[UUID],
    newNodeRequestNamesByUuid: Map[UUID, String],
    newEdgeRequestNamesByUuid: Map[UUID, String],
    customizedAssets: Set[UUID],
    priorCommit: WriteCommit,
    commit: WriteCommit,
    squashed: Boolean,
  ): WriteResponse =
    val nodesResponse = nodes
      .map(asset => asset.info.name -> asset)
      .toMap[UUID, Asset[?]]
    val usersResponse = nodes
      .flatMap(_.info.createdBy)
      .map(dto => Long2long(dto.id) -> dto)
      .toMap

    val edgesResponse = edges.map(edge => edge.name -> edge)

    val edgeIdsByTempName = newEdgeRequestNamesByUuid
      .map({ case (uuid, tempName) =>
        val edge = edges.find(_.name == uuid).get
        tempName -> edge.name
      })
    val nodeIdsByTempName = newNodeRequestNamesByUuid
      .map({ case (uuid, tempName) =>
        val asset = nodes.find(_.info.name == uuid).get
        tempName -> asset.info.name
      })
    WriteResponse(
      nodeIdsByTempName,
      edgeIdsByTempName,
      deletedEdges,
      nodesResponse,
      edgesResponse.toMap,
      usersResponse,
      customizedAssets,
      priorCommit,
      commit,
      squashed,
    )
  end apply
end WriteResponse

final case class WriteCommit(
  id: Long,
  created: Date,
)

object WriteCommit:
  def apply(ws: AttachedReadWorkspace): WriteCommit =
    WriteCommit(ws.commitId, ws.created.asDate)

  def apply(commit: Commit): WriteCommit =
    WriteCommit(commit.id, commit.createTime)
