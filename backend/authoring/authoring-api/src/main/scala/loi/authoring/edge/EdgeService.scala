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

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.workspace.ReadWorkspace

import java.util.UUID

@Service
trait EdgeService:

  final def load(ws: ReadWorkspace): EdgeLoader = new EdgeLoader(ws)

  final def loadST[S: AssetType, T: AssetType](ws: ReadWorkspace): EdgeLoaderST[S, T] = new EdgeLoaderST(ws)

  final def loadS[S: AssetType](ws: ReadWorkspace): EdgeLoaderS[S] = new EdgeLoaderS(ws)

  def loadEdgesByIds[S: AssetType, T: AssetType](
    workspace: ReadWorkspace,
    ids: Iterable[Long]
  ): List[AssetEdge[S, T]]

  def loadEdgesAnyTgtTypeByIds[S: AssetType](
    ws: ReadWorkspace,
    ids: Iterable[Long]
  ): List[AssetEdge[S, ?]]

  def loadEdgesAnyTypeById(
    workspace: ReadWorkspace,
    ids: Iterable[Long]
  ): List[AssetEdge.Any]

  final def loadOutEdgesT[T: AssetType](
    ws: ReadWorkspace,
    source: Asset[?],
    group: Group
  ): List[AssetEdge[?, T]] = loadOutEdgesT(ws, Seq(source), Set(group))

  final def loadOutEdgesT[T: AssetType](
    ws: ReadWorkspace,
    sources: Iterable[Asset[?]],
    groups: Set[Group]
  ): List[AssetEdge[?, T]] =
    loadOutEdges(ws, sources, groups, Set(AssetType[T].id)).flatMap[AssetEdge[?, T]](_.filterTgt[T])

  final def loadOutEdges(
    ws: ReadWorkspace,
    source: Asset[?],
    groupHead: Group,
    groupTail: Group*
  ): List[AssetEdge.Any] =
    loadOutEdges(ws, Seq(source), Set(groupHead) ++ groupTail)

  def loadOutEdges(
    ws: ReadWorkspace,
    sources: Iterable[Asset[?]],
    groups: Set[Group] = Set.empty,
    tgtTypes: Set[AssetTypeId] = Set.empty
  ): List[AssetEdge.Any]

  /** As above but using a workspace. */
  def stravaigeOutGraphs(
    graphs: Seq[TraverseFromSourcesAnyTargetType[UUID]],
    workspace: ReadWorkspace,
  ): TraversedGraph

  def stravaigeOutGraph(
    graph: TraverseFromSourcesAnyTargetType[UUID],
    workspace: ReadWorkspace,
  ): TraversedGraph = stravaigeOutGraphs(Seq(graph), workspace)

  /** Load typed parents of `targets`. S suffix for source type because the overloads are to ambiguous otherwise.
    *
    * @param groups
    *   filter parents by group. If empty, allow all groups.
    */
  def loadInEdgesS[S: AssetType](
    workspace: ReadWorkspace,
    targets: Iterable[Asset[?]],
    groups: Set[Group] = Set.empty
  ): List[AssetEdge[S, ?]]

  /** Load parents of `targets`.
    *
    * @param groups
    *   filter parents by group. If empty, allow all groups.
    * @param srcTypes
    *   filter parents by source node type. If empty, allow all parent types.
    */
  def loadInEdges(
    workspace: ReadWorkspace,
    targets: Iterable[Asset[?]],
    groups: Set[Group] = Set.empty,
    srcTypes: Set[AssetTypeId] = Set.empty,
  ): List[AssetEdge.Any]

  /** A lot of the same method seen here, but for when you want failures instead of just ignoring/skipping.
    */
  // for masochists
  def strict: StrictEdgeAccess

  class EdgeLoader(ws: ReadWorkspace):
    def byName(name: UUID): Option[AssetEdge.Any]          = byName(List(name)).headOption
    def byName(names: Iterable[UUID]): List[AssetEdge.Any] = byId(ws.getEdgeIds(names))
    def byId(ids: Iterable[Long]): List[AssetEdge.Any]     = loadEdgesAnyTypeById(ws, ids)

  class EdgeLoaderST[S: AssetType, T: AssetType](ws: ReadWorkspace):
    def byName(name: UUID): Option[AssetEdge[S, T]]          = byName(List(name)).headOption
    def byName(names: Iterable[UUID]): List[AssetEdge[S, T]] = byId(ws.getEdgeIds(names))
    def byId(ids: Iterable[Long]): List[AssetEdge[S, T]]     = loadEdgesByIds(ws, ids)

  class EdgeLoaderS[S: AssetType](ws: ReadWorkspace):
    def byName(names: Iterable[UUID]): List[AssetEdge[S, ?]] = byId(ws.getEdgeIds(names))
    def byId(ids: Iterable[Long]): List[AssetEdge[S, ?]]     = loadEdgesAnyTgtTypeByIds(ws, ids)
end EdgeService
