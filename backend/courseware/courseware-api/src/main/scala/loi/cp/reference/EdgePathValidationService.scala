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

package loi.cp.reference
import java.util.UUID
import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.workspace.{EdgeInfo, ReadWorkspace}
import scalaz.{NonEmptyList, \/}

@Service
trait EdgePathValidationService:

  def validate(
    ws: ReadWorkspace,
    edgeNames: List[EdgeInfo]
  ): NonEmptyList[EdgePathValidationError] \/ EdgePath

sealed trait EdgePathValidationError:
  val msg: String

object EdgePathValidationError:
  case class HeadAssNotCourseErr(
    edgeName: UUID,
    srcTypId: String
  ) extends EdgePathValidationError:
    val msg: String =
      s"The source of head edge name ${edgeName.toString} is not of type ${AssetTypeId.Course.entryName}; it is a $srcTypId."

  case class LastAssNotPlayable(
    edgeName: UUID,
    tgtTypId: String
  ) extends EdgePathValidationError:
    val msg: String =
      s"The target of last edge name ${edgeName.toString} is not playable and has no edge path; it is a $tgtTypId."

  case class IllogicalOrdrErr(
    edgeNames: NonEmptyList[UUID]
  ) extends EdgePathValidationError:
    val msg: String =
      s"The order of the edge name list has mismatched source and target ids: ${edgeNames.list.toList.mkString(",")}."

  case class EdgeNotExist(
    edgeName: UUID
  ) extends EdgePathValidationError:
    val msg: String = s"Edge name does not exist in workspace: ${edgeName.toString}"

  case class LastAssIsRemedtnRsrce(
    edgeName: UUID,
    tgtId: Long
  ) extends EdgePathValidationError:
    val msg: String = s"The target $tgtId of last edge name ${edgeName.toString} is a Remediation Resource."

  object EmptyEdgeList extends EdgePathValidationError:
    val msg: String = "Empty edge path."
end EdgePathValidationError
