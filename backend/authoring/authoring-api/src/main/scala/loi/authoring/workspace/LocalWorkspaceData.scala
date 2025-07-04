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

import com.fasterxml.jackson.annotation.JsonIgnore
import loi.authoring.edge.{EdgeAttrs, EdgeElem}
import loi.authoring.{EdgeInfo2, NodeInfo2}

import java.util.UUID
import scala.collection.View

/** Local, in-memory workspace data. Entirely backs the data used by the in-memory workspace. Partially backs the data
  * used by the persisted workspace.
  */
case class LocalWorkspaceData(
  nodeIdsByName: Map[UUID, Long],
  edgeInfosByName: Map[UUID, EdgeInfo],
  rootNodeNames: Set[UUID]
):

  @JsonIgnore
  lazy val nodeNamesById: Map[Long, UUID] = nodeIdsByName.map(_.swap)

  // i just want a sub-map of nodeIdsByName, but I don't want to iterate over it.
  @JsonIgnore
  lazy val rootNodeIdsByName: Map[UUID, Long] = rootNodeNames
    .flatMap(rootNodeName => nodeIdsByName.get(rootNodeName).map(rootNodeId => rootNodeName -> rootNodeId))
    .toMap

  // Map[SourceId, Seq[EdgeInfo]]
  @JsonIgnore
  lazy val outEdges: Map[Long, Seq[EdgeInfo]] = edgeInfosByName.values.toSeq.groupBy(_.sourceId)

  // Map[TargetId, Seq[EdgeInfo]]
  @JsonIgnore
  lazy val inEdges: Map[Long, Seq[EdgeInfo]] = edgeInfosByName.values.toSeq.groupBy(_.targetId)

  def getEdgeAttrs(name: UUID): Option[EdgeAttrs] = for
    ei      <- edgeInfosByName.get(name)
    srcName <- nodeNamesById.get(ei.sourceId)
    tgtName <- nodeNamesById.get(ei.targetId)
  yield EdgeAttrs(ei, srcName, tgtName)

  def getEdgeElem(name: UUID): Option[EdgeElem] = for
    ei      <- edgeInfosByName.get(name)
    srcName <- nodeNamesById.get(ei.sourceId)
    tgtName <- nodeNamesById.get(ei.targetId)
  yield EdgeElem(ei.id, EdgeAttrs(ei, srcName, tgtName))

  def outEdgeInfos(srcId: Long): View[EdgeInfo] = outEdges.getOrElse(srcId, Nil).view

  def outEdgeAttrs(srcName: UUID): View[EdgeAttrs] = for
    srcId   <- nodeIdsByName.get(srcName).view
    outEdge <- outEdgeInfos(srcId)
    tgtName <- nodeNamesById.get(outEdge.targetId)
  yield EdgeAttrs(outEdge, srcName, tgtName)

  def outEdgeElems(srcName: UUID): View[EdgeElem] = for
    srcId   <- nodeIdsByName.get(srcName).view
    ei      <- outEdgeInfos(srcId)
    tgtName <- nodeNamesById.get(ei.targetId)
  yield EdgeElem(ei.id, EdgeAttrs(ei, srcName, tgtName))

  def outEdgeInfos2(srcName: UUID): Iterable[EdgeInfo2] = for
    srcId   <- nodeIdsByName.get(srcName).view
    src      = NodeInfo2(srcName, srcId)
    outEdge <- outEdges.getOrElse(srcId, Nil).view
    tgtName <- nodeNamesById.get(outEdge.targetId)
    tgt      = NodeInfo2(tgtName, outEdge.targetId)
  yield EdgeInfo2(outEdge, src, tgt)

  def inEdgeInfos(tgtId: Long): View[EdgeInfo]   = inEdges.getOrElse(tgtId, Nil).view
  def inEdgeInfos(tgtName: UUID): View[EdgeInfo] = nodeIdsByName.get(tgtName).view.flatMap(inEdgeInfos)

  def inEdgeAttrs(tgtName: UUID): View[EdgeAttrs] = for
    tgtId   <- nodeIdsByName.get(tgtName).view
    inEdge  <- inEdgeInfos(tgtId)
    srcName <- nodeNamesById.get(inEdge.sourceId)
  yield EdgeAttrs(inEdge, srcName, tgtName)

  def inEdgeElems(tgtName: UUID): View[EdgeElem] = for
    tgtId   <- nodeIdsByName.get(tgtName).view
    inEdge  <- inEdgeInfos(tgtId)
    srcName <- nodeNamesById.get(inEdge.sourceId)
  yield EdgeElem(inEdge.id, EdgeAttrs(inEdge, srcName, tgtName))

  def addNode(name: UUID, id: Long): LocalWorkspaceData =
    copy(
      nodeIdsByName = nodeIdsByName.updated(name, id)
    )

  def addRootNode(name: UUID, id: Long): LocalWorkspaceData =
    copy(
      nodeIdsByName = nodeIdsByName.updated(name, id),
      rootNodeNames = rootNodeNames + name
    )

  def demoteRootNode(name: UUID): LocalWorkspaceData =
    copy(
      rootNodeNames = rootNodeNames - name
    )

  def promoteRootNode(name: UUID): LocalWorkspaceData =
    copy(
      rootNodeNames = rootNodeNames + name
    )

  def removeNode(name: UUID): LocalWorkspaceData =
    copy(
      rootNodeNames = rootNodeNames - name,
      nodeIdsByName = nodeIdsByName - name
    )

  def addEdge(name: UUID, info: EdgeInfo): LocalWorkspaceData =
    copy(
      edgeInfosByName = edgeInfosByName.updated(name, info)
    )

  def removeEdge(name: UUID): LocalWorkspaceData =
    copy(
      edgeInfosByName = edgeInfosByName - name
    )
end LocalWorkspaceData

object LocalWorkspaceData:
  val empty = LocalWorkspaceData(Map.empty, Map.empty, Set.empty)
