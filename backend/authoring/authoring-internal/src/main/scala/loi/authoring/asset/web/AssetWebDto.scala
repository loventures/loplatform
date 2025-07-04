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

package loi.authoring.asset.web

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import loi.authoring.asset.Asset
import loi.authoring.edge.{AssetEdge, Group}
import loi.cp.asset.edge.EdgeData

import java.util.UUID

case class AssetWebDto(asset: Asset[?], includes: Map[Group, Seq[AssetEdge[?, ?]]])

case class BulkFetchByIdDto(@JsonDeserialize(contentAs = classOf[java.lang.Long]) ids: java.util.List[java.lang.Long])

case class BulkFetchByNameDto(names: Seq[UUID])

case class SlimAssetWebDto(asset: Asset[?], includes: Map[Group, Seq[NewEdge]])

// corresponds to NewEdge in the front end
private[web] case class NewEdge(
  name: UUID,
  sourceName: UUID,
  targetName: UUID,
  data: EdgeData,
  group: Group,
  traverse: Boolean
)
