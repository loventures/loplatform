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

package loi.authoring.info

import loi.authoring.asset.Asset
import loi.authoring.edge.Group
import loi.cp.asset.edge.EdgeData

import java.util.UUID

final case class AssetPathElement(
  groupName: Group,
  edgeData: EdgeData,
  name: UUID,
  traverse: Boolean,
  asset: Asset[?]
)

object AssetPathElement:
  def apply(asset: Asset[?]): AssetPathElement =
    new AssetPathElement(null, null, null, false, asset)
