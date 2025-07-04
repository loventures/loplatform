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

package loi.authoring.node

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.workspace.ReadWorkspace
import loi.authoring.workspace.exception.WorkspaceNodeNotFound
import mouse.option.*

import java.util.UUID
import scala.util.Try

@Service
trait AssetNodeService:

  final def load(ws: ReadWorkspace): NodeLoader = new NodeLoader(ws)

  final def loadA[A: AssetType](ws: ReadWorkspace): NodeLoaderA[A] = new NodeLoaderA[A](ws)

  // workspace arg only for choosing assetnode or authoringnode
  def loadByIds(workspace: ReadWorkspace, ids: Iterable[Long]): List[Asset[?]]

  // guesses assetnode or authoringnode
  // temporarily separate from `loadRaw(Long)` so that I can audit callers of `loadRaw(Long)`
  // eventually becomes `loadRaw(Long)` that never guesses because everything is in authoringnode
  def loadRawByGuessing(id: Long): Option[Asset[?]]

  def loadByIdsA[A: AssetType](workspace: ReadWorkspace, ids: Iterable[Long]): List[Asset[A]]

  /** @param typeIds
    *   if empty, load all types
    */
  def loadByType(
    ws: ReadWorkspace,
    typeIds: Set[AssetTypeId] = Set.empty,
    limit: Option[Int] = Some(100)
  ): List[Asset[?]]

  class NodeLoader(ws: ReadWorkspace):

    def byName(name: UUID): Try[Asset[?]] = for
      id   <- ws.requireNodeId(name)
      node <- byId(id).toTry(WorkspaceNodeNotFound(id, ws.commitId))
    yield node

    def byName(names: Iterable[UUID]): Try[Seq[Asset[?]]] = Try {
      val ids = names.map(name => ws.requireNodeId(name).get)
      byId(ids)
    }

    def byId(id: Long): Option[Asset[?]]         = byId(List(id)).headOption
    def byId(ids: Iterable[Long]): Seq[Asset[?]] = loadByIds(ws, ids)

    /** @param typeIds
      *   if empty, all types are loaded
      */
    def byType(typeIds: Set[AssetTypeId] = Set.empty, limit: Option[Int] = Some(100)): List[Asset[?]] =
      loadByType(ws, typeIds, limit)

    def all(limit: Option[Int] = Some(100)): List[Asset[?]] = byType(Set.empty, limit)
  end NodeLoader

  class NodeLoaderA[A: AssetType](ws: ReadWorkspace):

    def byName(name: UUID): Try[Asset[A]] = for
      id   <- ws.requireNodeId(name)
      node <- byId(id).toTry(WorkspaceNodeNotFound(id, ws.commitId))
    yield node

    def byName(names: Iterable[UUID]): Try[Seq[Asset[A]]] = Try {
      val ids = names.map(name => ws.requireNodeId(name).get)
      byId(ids)
    }

    def byId(id: Long): Option[Asset[A]]          = byId(List(id)).headOption
    def byId(ids: Iterable[Long]): List[Asset[A]] = loadByIdsA(ws, ids)

    def all(limit: Option[Int] = Some(100)): List[Asset[A]] =
      loadByType(ws, Set(AssetType[A].id), limit).flatMap(_.filter[A])
  end NodeLoaderA
end AssetNodeService
