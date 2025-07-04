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

import loi.authoring.edge.EdgeAttrs
import loi.authoring.project.Commit2.Elem

import java.util.UUID
import scala.collection.View

sealed class Layer(
  val rawNodeIds: Map[UUID, Long],
  val rawEdgeIds: Map[UUID, Long],
  val projectId: Long,
  val depInfo: Commit2.Dep
) extends ElementContainer:

  def commitId: Long = depInfo.commitId

  private final lazy val rawNodeNames: Map[Long, UUID] = rawNodeIds.map(_.swap)

  override def getNodeId(name: UUID): Option[Long]   = rawNodeIds.get(name).filter(_ > 0)
  override def getNodeName(id: Long): Option[UUID]   = if id > 0 then rawNodeNames.get(id) else None
  override def getNodeElem(name: UUID): Option[Elem] = getNodeId(name).map(Elem.incl(name, _, projectId))
  override lazy val nodeNameIds: View[(UUID, Long)]  = rawNodeIds.view.filter(_._2 > 0)
  override lazy val nodeNames: View[UUID]            = nodeNameIds.map(_._1)
  override lazy val nodeElems: View[Elem]            = nodeNameIds.map { case (name, id) => Elem.incl(name, id, projectId) }

  override lazy val docEdgeIds: View[Long]               = rawEdgeIds.view.map(t => Math.abs(t._2))
  override def getFuzzyEdgeId(name: UUID): Option[Long]  = rawEdgeIds.get(name).filter(_ > 0)
  override lazy val fuzzyEdgeNameIds: View[(UUID, Long)] = rawEdgeIds.view.filter(_._2 > 0)
end Layer

final class LayerWithEdges(
  layer: Layer,
  docEdgeAttrs: Map[UUID, EdgeAttrs]
) extends EdgeElementContainer(docEdgeAttrs):

  val rawNodeIds: Map[UUID, Long] = layer.rawNodeIds
  val rawEdgeIds: Map[UUID, Long] = layer.rawEdgeIds
  val projectId: Long             = layer.projectId
  val depInfo: Commit2.Dep        = layer.depInfo

  // the downside of composition is this forwarding boilerplate
  // the upside is any layer lazy vals aren't discarded
  override def getNodeId(name: UUID): Option[Long]      = layer.getNodeId(name)
  override def getNodeName(id: Long): Option[UUID]      = layer.getNodeName(id)
  override def getNodeElem(name: UUID): Option[Elem]    = layer.getNodeElem(name)
  override def nodeNameIds: View[(UUID, Long)]          = layer.nodeNameIds
  override def nodeNames: View[UUID]                    = layer.nodeNames
  override def nodeElems: View[Elem]                    = layer.nodeElems
  override def docEdgeIds: View[Long]                   = layer.docEdgeIds
  override def getFuzzyEdgeId(name: UUID): Option[Long] = layer.getFuzzyEdgeId(name)
  override def fuzzyEdgeNameIds: View[(UUID, Long)]     = layer.fuzzyEdgeNameIds
end LayerWithEdges

object LayerWithEdges:
  val emptyLocal: LayerWithEdges = new LayerWithEdges(new Layer(Map.empty, Map.empty, 0L, Commit2.Dep.local), Map.empty)

case class FuzzyLayerBase(nodeIds: Map[UUID, Long], fuzzyEdgeIds: Map[UUID, Long])
