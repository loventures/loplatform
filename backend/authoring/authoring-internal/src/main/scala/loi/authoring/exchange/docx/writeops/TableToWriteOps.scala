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

package loi.authoring.exchange.docx.writeops

import com.learningobjects.cpxp.util.HtmlUtils.toPlaintext
import loi.asset.competency.service.CompetencyService
import loi.asset.contentpart.HtmlPart
import loi.authoring.edge.Group
import loi.authoring.exchange.docx.DocxErrors.{
  DocxValidationWarning,
  DuplicateFeedbackWarning,
  UnknownCompetencyWarning,
  UnknownRowTypeWarning
}
import loi.authoring.exchange.docx.DocxTuple.{CorrectFeedback, IncorrectFeedback}
import loi.authoring.exchange.docx.*
import loi.authoring.write.{AddEdge, SetEdgeOrder, WriteOp}
import org.apache.poi.xwpf.usermodel.{XWPFTableCell, XWPFTableRow}
import scalaz.std.either.*
import scalaz.std.tuple.*
import scaloi.syntax.collection.*
import scaloi.syntax.either.*

import java.util.UUID
import scala.jdk.CollectionConverters.*

private[docx] trait TableToWriteOps:
  def writes(table: Table, competencies: Map[String, UUID]): ValNel[Warned[List[WriteOp]]]

private[docx] object TableToWriteOps:

  import scalaz.std.list.*
  import scalaz.syntax.traverse.*

  case class BaseQuestionData(
    number: Int,
    html: String,
    plaintext: String,
    choiceRows: Seq[(XWPFTableRow, Int)],
    competencies: List[String],
    correctFeedback: Option[String],
    incorrectFeedback: Option[String],
  ):
    lazy val plainText: String = toPlaintext(html)
  end BaseQuestionData

  final def isTupleRow(row: XWPFTableRow): Boolean =
    row.getTableCells.size() == 2

  final def isTupleRow(key: DocxTuple)(row: XWPFTableRow): Boolean =
    isTupleRow(row) && tupleRowKey(row, blanks = true).contains(key);

  final def questionData(table: Table): BaseQuestionData =
    val rows = table.data.getRows.asScala.toList
    BaseQuestionData(
      number = table.idx + 1,
      html = table.data.text(0, 1).get,
      plaintext = table.data.getRow(0).getCell(1).getText,
      choiceRows =
        rows.zipWithIndex.tail.dropWhile(rowIdx => isTupleRow(rowIdx._1)).takeWhile(_._1.getTableCells.size == 3),
      competencies = tupleValues(rows, DocxTuple.Assesses, html = false).filterNot(_.isBlank),
      correctFeedback = tupleValues(rows, DocxTuple.CorrectFeedback, html = true).headOption.filterNot(_.isBlank),
      incorrectFeedback = tupleValues(rows, DocxTuple.IncorrectFeedback, html = true).headOption.filterNot(_.isBlank),
    )
  end questionData

  private def tupleValues(rows: List[XWPFTableRow], key: DocxTuple, html: Boolean): List[String] =
    rows
      .filter(isTupleRow(key))
      .map(row => if html then row.getCell(1).text() else row.getCell(1).getText.trim)

  final def validateChoices[T](table: Table, choiceRows: Seq[(XWPFTableRow, Int)])( //
    f: (Table, Int) => ValNel[T]
  ): ValNel[List[T]] =
    choiceRows
      .map { case (_, i) => f(table, i) }
      .toList
      .sequence

  final def feedback(cell: XWPFTableCell): Option[HtmlPart] =
    cell.text() match
      case s: String if s.nonEmpty => Some(HtmlPart(s))
      case _                       => None

  final def validateTupleRows(
    table: Table,
    blanks: Boolean = false
  ): Warned[Map[DocxTuple, List[XWPFTableCell]]] =
    val number = table.idx + 1
    val tuples = table.data.getRows.asScala.toList
      .filter(isTupleRow)
      .filterNot(_.getTableCells.asScala.forall(_.getText.isBlank))
    val result = tuples.foldLeft[Warned[List[(DocxTuple, XWPFTableCell)]]](Nil -> Nil) { case (warned, tuple) =>
      val pair = for
        tpe <- validateTupleType(number, tuple, blanks)
        _   <- List(CorrectFeedback, IncorrectFeedback).traverse(validateFeedback(number, tuples, _))
      yield tpe -> tuple.getCell(1)
      warned + pair
    }
    result.map(_.groupToMap)
  end validateTupleRows

  final def validateTupleType(
    number: Int,
    tuple: XWPFTableRow,
    blanks: Boolean
  ): Either[DocxValidationWarning, DocxTuple] =
    tupleRowKey(tuple, blanks).leftMap(UnknownRowTypeWarning(number, _))

  final def tupleRowKey(row: XWPFTableRow, blanks: Boolean): Either[String, DocxTuple] =
    val text = row.getCell(0).text(false).trim
    if blanks && text.startsWith("_") then Right(DocxTuple.Blank(text))
    else if text.matches("C\\d+|LO") then Right(DocxTuple.Assesses) // bizarreness
    else DocxTuple.withNameInsensitiveOption(text).toRight(text)

  final def validateFeedback(
    number: Int,
    tuples: List[XWPFTableRow],
    feedbackType: DocxTuple
  ): Either[DocxValidationWarning, Unit] =
    Either.cond(
      tuples.count(isTupleRow(feedbackType)) <= 1,
      (),
      DuplicateFeedbackWarning(number, feedbackType.entryName)
    )

  final def validateCompetencies(
    question: BaseQuestionData,
    competencies: Map[String, UUID]
  ): Warned[List[UUID]] =
    question.competencies.foldLeft[Warned[List[UUID]]](Nil -> Nil) { case (warned, title) =>
      warned + competencies
        .get(CompetencyService.normalize(title))
        .toRight(UnknownCompetencyWarning(question.number, title))
    }

  final def competencyWriteOps(
    name: UUID,
    competencies: List[UUID]
  ): List[WriteOp] =
    val addEdges = competencies.map(uuid =>
      AddEdge(
        sourceName = name,
        targetName = uuid,
        group = Group.Assesses,
        traverse = false,
      )
    )
    addEdges ??> (_ :+ SetEdgeOrder(
      sourceName = name,
      group = Group.Assesses,
      ordering = addEdges.map(_.name)
    ))
  end competencyWriteOps

  val OddDefaultFeedback = Some(HtmlPart(renderedHtml = Some("")))
end TableToWriteOps
