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

package loi.authoring.edge.web

import java.util.UUID

import loi.authoring.edge.Group
import loi.cp.asset.edge.EdgeData

/** Web DTO to carry info to add one or more usages.
  */
private[web] case class CreateEdgesWebDto(
  sourceName: UUID,
  group: Group,
  targets: Seq[EdgeTargetWebDto]
)

/** Web DTO to carry the target and data for a new edge
  */
private[web] case class EdgeTargetWebDto(targetName: UUID, traverse: Boolean, data: EdgeData)
