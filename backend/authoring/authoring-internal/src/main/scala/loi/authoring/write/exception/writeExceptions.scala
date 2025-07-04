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

package loi.authoring.write.exception

import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.edge.Group
import loi.authoring.write.WriteOp
import loi.cp.i18n.{AuthoringBundle, BundleMessage}
import scaloi.syntax.ClassTagOps.*

import java.util.UUID
import scala.reflect.ClassTag

sealed trait Logical extends UncheckedMessageException:
  val messageException: BundleMessage = AuthoringBundle.message("write.web.logic")

sealed trait OutdatedData extends UncheckedMessageException:
  val messageException: BundleMessage = AuthoringBundle.message("write.web.outdatedData")

case class WebMessage(messageException: BundleMessage)
    extends UncheckedMessageException(
      messageException
    )

case class AddNodeNameInCertainOtherOp(name: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.addNodeNameInCertainOtherOp", name.toString)
    )
    with Logical

case class AddEdgeNameInCertainOtherOp(name: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.addEdgeNameInCertainOtherOp", name.toString)
    )
    with Logical

case class DeleteEdgeNameInAnotherOp(name: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.deleteEdgeNameInAnotherOp", name.toString)
    )
    with Logical

case class DuplicateNameAmongSameOps[A <: WriteOp: ClassTag](name: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.duplicateNameAmongSameOps", name.toString, classTagClass[A].getSimpleName)
    )
    with Logical

case class DuplicateNameAmongAddOps(countOps: Int, countDupeNames: Int)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.duplicateNameAmongAddOps", int2Integer(countOps), int2Integer(countDupeNames))
    )
    with Logical

case class DuplicateNameInSetEdgeOrderOpOrdering(edgeName: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.duplicateNameInSetEdgeOrderOpOrdering", edgeName.toString)
    )
    with Logical

case class DuplicateSourceNameAndGroupAmongSetEdgeOrderOps(name: UUID, group: Group)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.duplicateSourceNameAndGroupAmongSetEdgeOrderOps", name.toString, group.entryName)
    )
    with Logical

case class DuplicateSourceTargetGroupAmongAddEdgeOps(sourceName: UUID, group: Group, targetName: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message(
        "write.duplicateSourceTargetGroupSetAmongAddEdgeOps",
        sourceName.toString,
        group.entryName,
        targetName.toString
      )
    )
    with Logical

case class ReplaceEdgeTargetOpHasSameTarget(edgeName: UUID, nodeName: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.replaceEdgeTargetOpHasSameTarget", edgeName.toString, nodeName.toString)
    )
    with Logical

case class EdgeTargetChangeOpResultsInSameTargetAndSource[A <: WriteOp: ClassTag](edgeName: UUID, nodeName: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message(
        "write.edgeTargetChangeOpResultsInSameTargetAndSource",
        edgeName.toString,
        nodeName.toString,
        classTagClass[A].getSimpleName
      )
    )
    with Logical

case class SetEdgeOrderOrderingDoesNotContainExpectedNames(
  sourceName: UUID,
  group: Group,
  expectedOrdering: Set[UUID],
  actualOrdering: Set[UUID]
) extends UncheckedMessageException(
      AuthoringBundle.message(
        "write.setEdgeOrderOrderingDoesNotContainExpectedNames",
        sourceName.toString,
        group.entryName,
        expectedOrdering.toString,
        actualOrdering.toString
      )
    )
    with OutdatedData

case class AddEdgeOpMissingSetEdgeOrderOp(edgeName: UUID, sourceName: UUID, group: Group)
    extends UncheckedMessageException(
      AuthoringBundle
        .message("write.addEdgeOpMissingSetEdgeOrder", edgeName.toString, sourceName.toString, group.entryName)
    )
    with Logical

case class AddEdgeNameMissingInCompanionSetEdgeOrderOp(edgeName: UUID, sourceName: UUID, group: Group)
    extends UncheckedMessageException(
      AuthoringBundle.message(
        "write.addEdgeNameMissingInCompanionSetEdgeOrderOp",
        edgeName.toString,
        sourceName.toString,
        group.entryName
      )
    )

case class AddEdgePositionAnchorNotFound(addEdgeName: UUID, sourceName: UUID, group: Group, anchorEdgeName: UUID)
    extends UncheckedMessageException(
      AuthoringBundle.message(
        "write.addEdgePositionAnchorNotFound",
        addEdgeName.toString,
        sourceName.toString,
        group.entryName,
        anchorEdgeName.toString
      )
    )
    with Logical

case class AddOpNamesExistInWorkspace(
  nodeNames: Set[UUID],
  edgeNames: Set[UUID]
) extends UncheckedMessageException(
      AuthoringBundle.message(
        "write.addOpNamesExistInWorkspace",
        nodeNames.mkString("{", ", ", "}"),
        edgeNames.mkString("{", ", ", "}"),
      )
    )
    with Logical

final case class ModifiedRemoteNodes(
  nodeNames: Set[UUID]
) extends UncheckedMessageException(
      AuthoringBundle.message(
        "write.modifiedRemoteNodes",
        nodeNames.mkString("{", ", ", "}"),
      )
    )
    with Logical

case class UnsupportedBranchyOpException(opType: String)
    extends UncheckedMessageException(AuthoringBundle.message("write.unsupportedBranchyOp", opType))

object UnsupportedBranchyOpException:
  def apply[A: ClassTag]: UnsupportedBranchyOpException = UnsupportedBranchyOpException(classTagClass[A].getSimpleName)

final case class ProjectUndergoingMaintenance()
    extends UncheckedMessageException(AuthoringBundle.message("write.web.maintenance"))

final case class CommitConflict(writeCommit: Long, localCommit: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("write.commitConflict", writeCommit.toString, localCommit.toString)
    )
