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

import loi.authoring.edge.service.exception.EdgeException.NoSuchEdge
import loi.authoring.workspace.ReadWorkspace
import scaloi.syntax.MapOps.*

import java.util.UUID
import scala.util.{Failure, Success, Try}

class BaseStrictEdgeAccess(
  edgeService: EdgeService,
) extends StrictEdgeAccess:

  override def loadEdgesAnyType(
    workspace: ReadWorkspace,
    names: Seq[UUID]
  ): Try[Seq[AssetEdge[?, ?]]] =
    val edges     = edgeService.load(workspace).byName(names)
    val edgeIndex = edges.groupBy(_.name).mapValuesEagerly(_.head)

    names.find(name => !edgeIndex.contains(name)) match
      case Some(name) => Failure(NoSuchEdge(name.toString))
      case None       => Success(edges)
  end loadEdgesAnyType
end BaseStrictEdgeAccess
