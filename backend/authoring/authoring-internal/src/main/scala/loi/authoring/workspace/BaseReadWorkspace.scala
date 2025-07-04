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

import loi.authoring.edge.{EdgeAttrs, EdgeElem}

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.View

class BaseReadWorkspace(
  val commitId: Long,
  val created: LocalDateTime,
  val createdBy: Long,
  val layered: Boolean,
  val data: LocalWorkspaceData
) extends ReadWorkspace:

  override final lazy val rootIds: Map[UUID, Long] = data.rootNodeIdsByName

  override final def getNodeId(name: UUID): Option[Long]  = data.nodeIdsByName.get(name)
  override final def getNodeName(id: Long): Option[UUID]  = data.nodeNamesById.get(id)
  override final lazy val nodeNameIds: View[(UUID, Long)] = data.nodeIdsByName.view
  override final lazy val nodeNames: View[UUID]           = nodeNameIds.map(_._1)

  override final def getEdgeId(name: UUID): Option[Long]         = data.edgeInfosByName.get(name).map(_.id)
  override final def getEdgeInfo(name: UUID): Option[EdgeInfo]   = data.edgeInfosByName.get(name)
  override final def getEdgeAttrs(name: UUID): Option[EdgeAttrs] = data.getEdgeAttrs(name)
  override final def getEdgeElem(name: UUID): Option[EdgeElem]   = data.getEdgeElem(name)
  override final lazy val edgeIds: View[Long]                    = data.edgeInfosByName.view.map(_._2.id)
  override final lazy val edgeNames: View[UUID]                  = data.edgeInfosByName.view.map(_._1)
  override final lazy val edgeInfos: View[EdgeInfo]              = data.edgeInfosByName.view.map(_._2)

  override final def outEdgeInfos(srcId: Long): View[EdgeInfo]    = data.outEdgeInfos(srcId)
  override final def outEdgeAttrs(srcName: UUID): View[EdgeAttrs] = data.outEdgeAttrs(srcName)
  override final def outEdgeElems(srcName: UUID): View[EdgeElem]  = data.outEdgeElems(srcName)
  override final def inEdgeInfos(tgtId: Long): View[EdgeInfo]     = data.inEdgeInfos(tgtId)
  override final def inEdgeInfos(tgtName: UUID): View[EdgeInfo]   = data.inEdgeInfos(tgtName)
  override final def inEdgeAttrs(tgtName: UUID): View[EdgeAttrs]  = data.inEdgeAttrs(tgtName)
  override final def inEdgeElems(tgtName: UUID): View[EdgeElem]   = data.inEdgeElems(tgtName)
end BaseReadWorkspace
