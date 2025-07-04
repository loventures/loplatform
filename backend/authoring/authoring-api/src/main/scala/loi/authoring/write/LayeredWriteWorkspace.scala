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

import loi.authoring.ProjectId
import loi.authoring.edge.EdgeAttrs
import loi.authoring.project.{Commit2, FuzzyLayerBase, Project2}
import loi.authoring.workspace.{ProjectWorkspace, WriteWorkspace}

import java.util.UUID

/** @param layerBases
  *   everything the light of our project touches, even elems we haven't claimed yet.
  */
final class LayeredWriteWorkspace(
  project: Project2,
  doc: Commit2.ComboDoc,
  docEdgeAttrs: Map[UUID, EdgeAttrs],
  val layerBases: Map[ProjectId, FuzzyLayerBase]
) extends ProjectWorkspace(project, project.head, doc, docEdgeAttrs)
    with WriteWorkspace:

  val kfDoc: Commit2.Doc            = doc.kf
  val driftDoc: Option[Commit2.Doc] = doc.drift

  // like getNodeId but only returns Some if name is an unclaimed remote node
  private[write] def getUnclaimedNode(nodeName: UUID): Option[UnclaimedNode] =
    if containsNode(nodeName) then None
    else
      val unclaimedNodes = for
        (projectId, layerBase) <- layerBases
        nodeId                 <- layerBase.nodeIds.get(nodeName)
      yield UnclaimedNode(nodeName, nodeId, projectId, doc.deps(projectId).commitId, layerBase)

      // as ever, I don't exactly know how two layers could contain the same name,
      // but if they did, we always take the highest layer's match
      unclaimedNodes.headOption

  override def knowsNode(nodeName: UUID): Boolean = containsNode(nodeName) ||
    layerBases.exists(_._2.nodeIds.contains(nodeName))

  override def knowsEdge(nodeName: UUID): Boolean = containsEdge(nodeName) ||
    layerBases.exists(_._2.fuzzyEdgeIds.contains(nodeName))
end LayeredWriteWorkspace

case class UnclaimedNode(name: UUID, id: Long, theirProjectId: Long, theirCommitId: Long, layerBase: FuzzyLayerBase)
