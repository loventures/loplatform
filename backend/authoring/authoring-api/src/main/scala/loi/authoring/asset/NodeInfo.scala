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

package loi.authoring.asset

import loi.asset.TitleProperty
import loi.authoring.asset.factory.AssetTypeId

import java.util.{Date, UUID}

/** Metadata about a node. Just like `AssetInfo` but without the create user. And it has the title
  */
case class NodeInfo(
  id: Long,
  name: UUID,
  typeId: AssetTypeId,
  created: Date,
  title: String,
  modified: Date
)

object NodeInfo:

  def fromAsset(asset: Asset[?]): NodeInfo =
    NodeInfo(
      asset.info.id,
      asset.info.name,
      asset.info.typeId,
      asset.info.created,
      TitleProperty.fromNode(asset).getOrElse(""),
      asset.info.modified
    )
end NodeInfo
