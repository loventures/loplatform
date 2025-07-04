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

import loi.authoring.branch.Branch
import loi.authoring.write.AddEdge

import java.util.UUID

// existence of this service object is barely justified because the entity type is nearly identical, but this gives us
//   1. the copy function that all case classes have, which the tests use to swap the branch
//   2. a detached object, which the entity is not. With this service object, service callers don't have the opportunity
//      to forget to Session.evict before mutating the entity.
//   3. not being a sore thumb amongst the rest of the authoring entities, none of which intend the session-attachable
//      entity types for general use.
final case class DurableEdge(sourceName: UUID, targetName: UUID, group: Group, name: UUID, branch: Branch)

object DurableEdge:

  def from(edge: AssetEdge[?, ?], branch: Branch): DurableEdge =
    DurableEdge(edge.source.info.name, edge.target.info.name, edge.group, edge.name, branch)

  final case class Key(sourceName: UUID, targetName: UUID, group: Group)

  object Key:
    def from(ae: AddEdge): Key        = Key(ae.sourceName, ae.targetName, ae.group)
    def from(e: AssetEdge[?, ?]): Key = Key(e.source.info.name, e.target.info.name, e.group)

  def keyMapOf(edges: AssetEdge[?, ?]*): Map[DurableEdge.Key, UUID] = edges.view.map(e => Key.from(e) -> e.name).toMap
end DurableEdge
