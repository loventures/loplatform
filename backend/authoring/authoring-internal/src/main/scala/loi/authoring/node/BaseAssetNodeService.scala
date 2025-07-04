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
import loi.authoring.node.store.NodeDao2
import loi.authoring.workspace.ReadWorkspace

@Service
class BaseAssetNodeService(
  nodeDao2: NodeDao2,
) extends AssetNodeService:

  override def loadByIds(
    workspace: ReadWorkspace,
    ids: Iterable[Long]
  ): List[Asset[?]] = loadAnyTypeLayered(ids)

  override def loadRawByGuessing(id: Long): Option[Asset[?]] = loadAnyTypeLayered(Seq(id)).headOption

  override def loadByIdsA[A: AssetType](workspace: ReadWorkspace, ids: Iterable[Long]): List[Asset[A]] = loadALayered(
    ids
  )

  override def loadByType(ws: ReadWorkspace, typeIds: Set[AssetTypeId], limit: Option[Int]): List[Asset[?]] =
    val entities = if typeIds.isEmpty then
      val ids = limit.map(l => ws.nodeIds.take(l)).getOrElse(ws.nodeIds)
      nodeDao2.load(ids)
    else nodeDao2.loadAllByTypeIds(ws.nodeIds, typeIds, limit)

    entities.map(entity =>
      val assetType = NodeDao2.assetTypeOrThrow(entity)
      NodeDao2.entityToAsset(entity)(using assetType)
    )

  private def loadAnyTypeLayered(ids: Iterable[Long]): List[Asset[?]] =
    nodeDao2
      .load(ids)
      .map(entity =>
        val assetType = NodeDao2.assetTypeOrThrow(entity)
        NodeDao2.entityToAsset(entity)(using assetType)
      )

  private def loadALayered[A: AssetType](ids: Iterable[Long]): List[Asset[A]] =
    nodeDao2
      .load(ids)
      .filter(_.typeId == AssetType[A].id.entryName)
      .map(e => NodeDao2.entityToAsset[A](e))
end BaseAssetNodeService
