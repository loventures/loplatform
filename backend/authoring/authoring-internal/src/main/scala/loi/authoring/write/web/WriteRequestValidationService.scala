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

package loi.authoring.write.web

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.workspace.WriteWorkspace
import loi.authoring.write.WriteOp
import loi.cp.i18n.AuthoringBundle
import scalaz.{NonEmptyList, \/}

import java.util.UUID

@Service
// Because AuthoringApp needs to see it, the trait and class are not private[web]
trait WriteRequestValidationService:

  // This needs the workspace to load asset type ids for setNodeData
  def validate(
    reqs: Seq[WriteRequest],
    ws: WriteWorkspace
  ): NonEmptyList[WriteRequestValidationError] \/ WriteValidationBundle

case class WriteValidationBundle(
  writeOps: List[WriteOp],
  newRequestNamesByUuid: NewRequestNamesByUuid
)

case class NewRequestNamesByUuid(
  nodes: Map[UUID, String],
  edges: Map[UUID, String]
)

sealed trait WriteRequestValidationError:
  val msg: String

object WriteRequestValidationError:

  case class DuplicateNameAmongAddRequests(
    numExpectedUniqueNames: Long,
    numActualUniqueNames: Long
  ) extends WriteRequestValidationError:
    val msg: String = s"Duplicate name among Add* requests error: " +
      s"expected $numExpectedUniqueNames unique names, actual unique names: $numActualUniqueNames"

  case class NodeNameAlreadyInWorkspace(name: String) extends WriteRequestValidationError:
    val msg: String = s"Node name is already in the workspace: $name"

  case class NoSuchEdgeInWorkspaceOrAddEdgeRequest(name: String) extends WriteRequestValidationError:
    val msg: String = s"No such edge in workspace or AddEdge request: $name"

  case class NoSuchNodeInWorkspaceOrAddNodeRequest(name: String) extends WriteRequestValidationError:
    val msg: String = s"No such node in workspace or AddNode request: $name"

  case class NoSuchType(typeId: AssetTypeId) extends WriteRequestValidationError:
    val msg: String = s"No such asset type: ${typeId.entryName}"

  case class DeserFailure(msg: String) extends WriteRequestValidationError
end WriteRequestValidationError

case class WriteRequestValidationException(errors: NonEmptyList[WriteRequestValidationError])
    extends UncheckedMessageException(
      AuthoringBundle.message("write.requestErrors", errors.list.toList.map(_.msg).mkString(","))
    )
