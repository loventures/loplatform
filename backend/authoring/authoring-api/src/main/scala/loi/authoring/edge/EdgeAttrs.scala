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

import loi.authoring.workspace.EdgeInfo

import java.util.UUID

final case class EdgeAttrs(
  name: UUID,
  srcName: UUID,
  tgtName: UUID,
  localId: UUID,
  group: Group,
  position: Int,
  traverse: Boolean,
)

object EdgeAttrs:
  def apply(info: EdgeInfo, srcName: UUID, tgtName: UUID): EdgeAttrs = EdgeAttrs(
    info.name,
    srcName,
    tgtName,
    info.edgeId,
    info.group,
    info.position.toInt,
    info.traverse,
  )
end EdgeAttrs

final case class EdgeElem(
  id: Long,
  attrs: EdgeAttrs
):
  def name: UUID        = attrs.name
  def srcName: UUID     = attrs.srcName
  def tgtName: UUID     = attrs.tgtName
  def grp: Group        = attrs.group
  def position: Int     = attrs.position
  def localId: UUID     = attrs.localId
  def traverse: Boolean = attrs.traverse
end EdgeElem
