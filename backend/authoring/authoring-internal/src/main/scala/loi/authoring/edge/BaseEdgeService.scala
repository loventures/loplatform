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

package loi.authoring.edge

import cats.syntax.option.*
import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.store.*
import loi.authoring.node.AssetNodeService
import loi.authoring.node.store.{NodeDao2, NodeEntity2}
import loi.authoring.project.Commit2
import loi.authoring.workspace.ReadWorkspace
import scaloi.syntax.collection.*

import java.util.UUID
import scala.annotation.tailrec
import scala.collection.mutable

@Service
class BaseEdgeService(
  edgeDao2: EdgeDao2,
  nodeDao2: NodeDao2,
  nodeService: AssetNodeService,
) extends EdgeService:

  override def loadEdgesByIds[S: AssetType, T: AssetType](
    workspace: ReadWorkspace,
    ids: Iterable[Long]
  ): List[AssetEdge[S, T]] = loadLayered(workspace, ids)

  override def loadEdgesAnyTgtTypeByIds[S: AssetType](
    workspace: ReadWorkspace,
    ids: Iterable[Long]
  ): List[AssetEdge[S, ?]] = loadAnyTgtTypeLayered(workspace, ids)

  override def loadEdgesAnyTypeById(
    workspace: ReadWorkspace,
    ids: Iterable[Long]
  ): List[AssetEdge.Any] = loadAnyTypeLayered(workspace, ids)

  override def loadOutEdges(
    ws: ReadWorkspace,
    sources: Iterable[Asset[?]],
    groups: Set[Group],
    tgtTypes: Set[AssetTypeId]
  ): List[AssetEdge.Any] =
    val edgeIds  = sources.view
      .flatMap(src => ws.outEdgeInfos(src.info.id))
      .filter(e => groups.isEmpty || groups.contains(e.group))
      .map(_.id)
      .toSet
    val edges    = load(ws).byId(edgeIds)
    val unsorted =
      if tgtTypes.isEmpty then edges
      else edges.filter(e => tgtTypes.contains(e.target.info.typeId))

    unsorted.sortBy(e => (e.source.info.id, e.group.entryName, e.position))
  end loadOutEdges

  override def stravaigeOutGraphs(
    traversals: Seq[TraverseFromSourcesAnyTargetType[UUID]],
    ws: ReadWorkspace,
  ): TraversedGraph =

    val edgeIds = mutable.Set.empty[Long]

    @tailrec
    def loop(gs: List[GroupStep], names: List[UUID]): Unit = gs match
      case step :: tail =>
        val targetNames = for
          sourceName <- names
          sourceId    = ws.nodeId(sourceName)
          edge       <- ws.outEdgeInfos(sourceId)
          if step.groups.contains(edge.group)
          _           = edgeIds.add(edge.id)
        yield ws.nodeName(edge.targetId)
        loop(tail, targetNames)

      case Nil =>

    traversals foreach { traversal =>
      loop(traversal.steps.collect({ case gs: GroupStep => gs }).toList, traversal.sources.toList)
    }

    val edges   = load(ws).byId(edgeIds)
    val nodeIds = traversals.flatMap(_.sources).map(ws.nodeId)
    val nodes   = nodeService.load(ws).byId(nodeIds)

    TraversedGraph(nodes, edges)
  end stravaigeOutGraphs

  override def loadInEdgesS[S: AssetType](
    ws: ReadWorkspace,
    targets: Iterable[Asset[?]],
    groups: Set[Group] = Set.empty
  ): List[AssetEdge[S, ?]] =
    loadInEdges(ws, targets, groups, Set(AssetType[S].id))
      .flatMap[AssetEdge[S, ?]](_.filterSrc[S])

  override def loadInEdges(
    ws: ReadWorkspace,
    targets: Iterable[Asset[?]],
    groups: Set[Group],
    srcTypes: Set[AssetTypeId]
  ): List[AssetEdge.Any] =

    val edgeIds = targets.view
      .flatMap(tgt => ws.inEdgeInfos(tgt.info.id))
      .filter(e => groups.isEmpty || groups.contains(e.group))
      .map(_.id)
      .toSet

    val edges    = load(ws).byId(edgeIds)
    val unsorted =
      if srcTypes.isEmpty then edges
      else edges.filter(e => srcTypes.contains(e.source.info.typeId))

    // sorting by position is nonsensical since a source-group can only contain a
    // target once, so we source by source id lastly for determinism
    unsorted.sortBy(e => (e.target.info.id, e.group.entryName, e.source.info.id))
  end loadInEdges

  // loads all edges, included those excluded and those unexpressed
  def loadEdgeAttrs(comboDoc: Commit2.ComboDoc): Map[UUID, EdgeAttrs] =
    edgeDao2.load(comboDoc.docEdgeIds).view.map(e => e.name -> e.toEdgeAttrs).toMap

  private def loadAnyTypeLayered(ws: ReadWorkspace, ids: Iterable[Long]): List[AssetEdge.Any] =
    val (edges, nodes) = loadWithVertices(ws, ids)

    edges.map(entity =>
      val source = NodeDao2.entityToAssetE(nodes(entity.sourceName))
      val target = NodeDao2.entityToAssetE(nodes(entity.targetName))
      EdgeDao2.entityToEdge(entity, source, target)
    )

  private def loadAnyTgtTypeLayered[S: AssetType](ws: ReadWorkspace, ids: Iterable[Long]): List[AssetEdge[S, ?]] =
    val (edges, nodes) = loadWithVertices(ws, ids)

    edges.flatMap(entity =>
      val src = nodes(entity.sourceName)
      val tgt = nodes(entity.targetName)

      if src.typeId == AssetType[S].id.entryName then
        val source = NodeDao2.entityToAsset[S](src)
        val target = NodeDao2.entityToAssetE(tgt)
        EdgeDao2.entityToEdge(entity, source, target).some
      else None
    )
  end loadAnyTgtTypeLayered

  private def loadLayered[S: AssetType, T: AssetType](ws: ReadWorkspace, ids: Iterable[Long]): List[AssetEdge[S, T]] =
    val (edges, nodes) = loadWithVertices(ws, ids)

    edges.flatMap(entity =>

      val src = nodes(entity.sourceName)
      val tgt = nodes(entity.targetName)

      if src.typeId == AssetType[S].id.entryName && tgt.typeId == AssetType[T].id.entryName then
        val source = NodeDao2.entityToAsset[S](src)
        val target = NodeDao2.entityToAsset[T](tgt)
        EdgeDao2.entityToEdge(entity, source, target).some
      else None
    )
  end loadLayered

  private def loadWithVertices(ws: ReadWorkspace, ids: Iterable[Long]): (List[EdgeEntity2], Map[UUID, NodeEntity2]) =
    val edges   = edgeDao2.load(ids)
    val nodeIds = edges.foldLeft(Set.empty[Long]) { case (acc, e) =>
      val sourceId = ws.nodeId(e.sourceName)
      val targetId = ws.nodeId(e.targetName)
      acc + sourceId + targetId
    }

    val nodes = nodeDao2.load(nodeIds).groupUniqBy(_.name)
    (edges, nodes)
  end loadWithVertices

  override val strict: StrictEdgeAccess = new BaseStrictEdgeAccess(this)
end BaseEdgeService
