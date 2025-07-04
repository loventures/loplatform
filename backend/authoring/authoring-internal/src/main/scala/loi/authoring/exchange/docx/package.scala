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

package loi.authoring.exchange

import com.learningobjects.cpxp.util.HtmlUtils.toPlaintext
import loi.authoring.exchange.docx.format.RunTag
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.text.StringEscapeUtils
import org.apache.poi.xwpf.usermodel.*
import scalaz.Validation.FlatMap.*
import scalaz.ValidationNel
import scalaz.std.list.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.traverse.*
import scaloi.syntax.validation.*

import scala.collection.BitSet
import scala.jdk.CollectionConverters.*

package object docx:

  import DocxErrors.{DocxValidationError as Err, *}
  import loi.authoring.exchange.docx.format.DocxFormat.html

  private[docx] type ValNel[T] = ValidationNel[Err, T]

  /** A tuple of a list of warnings and a value [T]. */
  private[docx] type Warned[T] = (List[DocxValidationWarning], T)

  private[docx] def cols(
    table: Table,
    rowIdx: Int,
    expected: Int,
    required: BitSet = BitSet.empty
  ): ValNel[List[XWPFTableCell]] =
    for
      _     <- tableRow(table, rowIdx)
      _     <- colCount(table, rowIdx, expected)
      cells <- textCols(table, rowIdx, required)
    yield cells

  private[docx] def countValNel[T](ts: Seq[T], g: Int => Boolean)(f: T => Boolean, err: => Err) =
    ts.count(f).validNelWhen[Err](g, err)

  private[docx] def needXRows(table: Table, x: Int) =
    val size = table.data.getRows.size()
    size.validNelWhen[Err](_ >= x, InsufficientRowsError(table.idx + 1, x, size))

  private[docx] def maxChoices(question: Int, expected: Int, actual: Int) =
    actual.validNelWhen[Err](
      _ <= expected,
      TooManyChoicesError(question, expected, actual)
    )

  private[docx] case class Table(data: XWPFTable, idx: Int)

  private[docx] implicit class TableOps(private val table: XWPFTable) extends AnyVal:
    def text(row: Int, col: Int, formatted: Boolean = true): Option[String] =
      for
        r <- Option(table.getRow(row))
        c <- Option(r.getCell(col))
      yield c.text(formatted)

  private[docx] implicit class CellOps(private val cell: XWPFTableCell) extends AnyVal:
    def text(formatted: Boolean = true): String =
      if formatted then html(cell.getBodyElements.asScala.toSeq) else StringEscapeUtils.escapeHtml4(cell.getText.trim)

  private def tableRow(table: Table, rowIdx: Int) =
    table.data
      .getRow(rowIdx)
      .validNelWhen[Err](
        _ != null,
        MissingRowError(table.idx + 1, rowIdx + 1)
      )

  private def colCount(table: Table, rowIdx: Int, expected: Int) =
    val cols = table.data.getRow(rowIdx).getTableCells.size()
    cols.validNelWhen[Err](
      _ == expected,
      ColumnCountError(table.idx + 1, rowIdx + 1, expected, cols)
    )

  private def textCols(table: Table, rowIdx: Int, required: BitSet) =
    table.data
      .getRow(rowIdx)
      .getTableCells
      .asScala
      .zipWithIndex
      .map { case (c, i) =>
        c.validNelWhen[Err](
          !required(i) || _.text(false).nonEmpty,
          BlankTextError(table.idx + 1, rowIdx + 1, i + 1)
        )
      }
      .toList
      .sequence

  private[docx] case class Surround private (open: Seq[String], close: Seq[String])

  private[docx] object Surround:
    def apply(r: XWPFRun): Surround =
      val (open, close) = RunTag.values.flatMap { tr =>
        tr.inRun(r).option((s"<${tr.entryName}>", s"</${tr.entryName}>"))
      }.unzip
      Surround(open, close.reverse)

  private[docx] def isCorrectChoice(c: XWPFTableCell) =
    !isBlank(toPlaintext(CellOps(c).text(formatted = false)))

  // this is quite inefficient, appending to the lists, but order matters and these lists are short
  private[docx] implicit class TupledListOps[A, B](val self: (List[A], List[B])):
    def +(either: Either[A, B]): (List[A], List[B]) = either match
      case Left(a)  => (self._1 ::: a :: Nil, self._2)
      case Right(b) => (self._1, self._2 ::: b :: Nil)
end docx
