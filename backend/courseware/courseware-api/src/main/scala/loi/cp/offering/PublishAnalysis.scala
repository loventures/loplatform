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

package loi.cp.offering

import com.github.tototoshi.csv.CSVWriter
import loi.cp.content.CourseContent
import loi.cp.lwgrade.GradeColumn
import loi.cp.reference.EdgePath
import scaloi.data.ListTree

import java.io.File
import java.util.UUID
import scala.util.Using

final case class PublishAnalysis(
  numStaleSections: Int,
  lisResultChanges: List[PublishAnalysis.LineItemContainer]
):

  def writeCsv(file: File): Unit =
    Using.resource(CSVWriter.open(file)) { csv =>
      csv.writeRow(
        Seq(
          "Change",
          "Section",
          "LIS Line Item ID",
          "LIS Assigned Activity ID",
          "Title",
          "Location",
          "Points Possible",
          "Is For Credit",
          "Prev Points Possible",
          "Prev Is For Credit"
        )
      )

      for
        container <- lisResultChanges
        create    <- container.creates
      do
        csv.writeRow(
          Seq(
            "create",
            container.groupId,
            null,
            create.edgePath.toString,
            create.title,
            create.parentTitle.orNull,
            create.pointsPossible.toString,
            create.isForCredit.toString,
            null,
            null
          )
        )
      end for

      for
        container <- lisResultChanges
        update    <- container.updates
      do
        csv.writeRow(
          Seq(
            "update",
            container.groupId,
            update.lineItemId,
            update.edgePath.toString,
            update.title,
            update.parentTitle.orNull,
            update.pointsPossible.toString,
            update.isForCredit.toString,
            update.prevPointsPossible.map(_.toString).orNull,
            update.prevIsForCredit.map(_.toString).orNull
          )
        )
      end for

      for
        container <- lisResultChanges
        delete    <- container.deletes
      do
        csv.writeRow(
          Seq(
            "delete",
            container.groupId,
            delete.lineItemId,
            delete.edgePath.toString,
            delete.title,
            delete.parentTitle.orNull,
            delete.pointsPossible.toString,
            delete.isForCredit.toString,
            null,
            null
          )
        )
      end for
    }
end PublishAnalysis

object PublishAnalysis:

  final case class LineItemContainer(
    sectionId: Long,
    groupId: String,
    lineItemsUrl: String,
    systemId: Long,
    creates: List[CreateLineItem],
    updates: List[UpdateLineItem],
    deletes: List[DeleteLineItem],
  ):
    def hasChanges: Boolean = creates.nonEmpty || updates.nonEmpty || deletes.nonEmpty
  end LineItemContainer

  sealed trait LineItemChange:
    def name: UUID
    def edgePath: EdgePath
    def title: String
    def pointsPossible: BigDecimal
    def isForCredit: Boolean

  object LineItemChange:
    def headAndParent[A](path: List[ListTree[A]]): (A, Option[A]) =
      val head   = path.head.rootLabel
      val parent = path.tail.headOption.map(_.rootLabel)
      (head, parent)

  final case class CreateLineItem(
    name: UUID,
    edgePath: EdgePath,
    title: String,
    parentTitle: Option[String],
    pointsPossible: BigDecimal,
    isForCredit: Boolean
  ) extends LineItemChange

  object CreateLineItem:
    def from(path: List[ListTree[CourseContent]], column: GradeColumn): CreateLineItem =
      val (content, parent) = LineItemChange.headAndParent(path)
      CreateLineItem(
        content.name,
        content.edgePath,
        content.title,
        parent.map(_.title),
        column.pointsPossible,
        column.isForCredit
      )
    end from
  end CreateLineItem

  final case class UpdateLineItem(
    name: UUID,
    edgePath: EdgePath,
    title: String,
    parentTitle: Option[String],
    pointsPossible: BigDecimal,
    isForCredit: Boolean,
    lineItemId: String,
    prevPointsPossible: Option[BigDecimal], // only Some if different
    prevIsForCredit: Option[Boolean]        // only Some if different
  ) extends LineItemChange

  object UpdateLineItem:
    def from(
      path: List[ListTree[CourseContent]],
      column: GradeColumn,
      lineItemId: String,
      prevPp: Option[BigDecimal],
      prevFc: Option[Boolean]
    ): UpdateLineItem =
      val (content, parent) = LineItemChange.headAndParent(path)
      UpdateLineItem(
        content.name,
        content.edgePath,
        content.title,
        parent.map(_.title),
        column.pointsPossible,
        column.isForCredit,
        lineItemId,
        prevPp,
        prevFc
      )
    end from
  end UpdateLineItem

  final case class DeleteLineItem(
    name: UUID,
    edgePath: EdgePath,
    title: String,
    parentTitle: Option[String],
    pointsPossible: BigDecimal,
    isForCredit: Boolean,
    lineItemId: String,
  ) extends LineItemChange

  object DeleteLineItem:
    def from(path: List[ListTree[CourseContent]], column: GradeColumn, lineItemId: String): DeleteLineItem =
      val (content, parent) = LineItemChange.headAndParent(path)
      DeleteLineItem(
        content.name,
        content.edgePath,
        content.title,
        parent.map(_.title),
        column.pointsPossible,
        column.isForCredit,
        lineItemId
      )
    end from
  end DeleteLineItem
end PublishAnalysis
