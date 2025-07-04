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

package loi.authoring.copy

import loi.authoring.edge.Group

import java.util.UUID

package object web:
  case class BronchAsset(branch: Long, node: UUID):
    def plural: BronchAssets = BronchAssets(branch, List(node))

  case class CopyWebDto(newTitle: String)

  case class ContentCopyDto(
    source: BronchAsset,
    target: BronchAsset,
    group: Group,
    beforeEdge: Option[UUID],
  )

  case class BronchAssets(branch: Long, nodes: List[UUID])

  case class BulkContentCopyDto(
    source: BronchAssets,
    target: BronchAsset,
    group: Group,
    beforeEdge: Option[UUID],
  )
end web
