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

package loi.authoring.exchange.docx

import enumeratum.EnumEntry.CapitalWords
import enumeratum.{Enum, EnumEntry}
import loi.asset.contentpart.HtmlPart
import loi.asset.question.*
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.exchange.docx.DocxQuestionType.*
import loi.authoring.exchange.docx.format.HtmlSink
import org.apache.poi.xwpf.usermodel.{XWPFDocument, XWPFTable, XWPFTableCell}
import scaloi.syntax.collection.*

import java.math.BigInteger
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

private[exchange] object DocxQuestionAppender:
  import DocxTuple.*

  def appendQuestions(doc: XWPFDocument, questions: Seq[Asset[?]], competencyMap: Map[UUID, Seq[Asset[?]]]): Unit =
    questions.zipWithIndex foreach { case (question, i) =>
      val competencies = competencyMap.getOrElse(question.info.name, Seq.empty).flatMap(_.title)
      question match
        case TrueFalseQuestion.Asset(tf)        =>
          appendChoiceQuestion(i, tf.data.questionContent, TrueFalse, competencies, doc)
        case MultipleChoiceQuestion.Asset(mc)   =>
          appendChoiceQuestion(i, mc.data.questionContent, MultipleChoice, competencies, doc)
        case MultipleSelectQuestion.Asset(ms)   =>
          appendChoiceQuestion(i, ms.data.questionContent, MultipleSelect, competencies, doc)
        case FillInTheBlankQuestion.Asset(fitb) =>
          appendFitBQuestion(i, fitb.data.questionContent, competencies, doc)
        // TODO: Essay and Matching
        case _                                  =>
      end match
    }

  private def appendChoiceQuestion(
    i: Int,
    data: ChoiceQuestionContent,
    qType: DocxQuestionType,
    competencies: Seq[String],
    doc: XWPFDocument
  ): Unit =
    implicit val table: XWPFTable = this.table(doc)

    appendHeader(i, data.questionComplexText.html, qType)

    (competencies || Seq("")) foreach { title =>
      appendTuple(Assesses, title)
    }

    data.choices foreach { cc =>
      val cells = addRow(Seq(10, 30, 60)).getTableCells
      cells.get(1).setHtml(cc.choiceContent)
      if cc.correct then
        cells.get(0).setText("*")
        cells.get(2).setHtml(cc.correctChoiceFeedback)
      else cells.get(2).setHtml(cc.incorrectChoiceFeedback)
    }

    appendFooter(data.richCorrectAnswerFeedback, data.richIncorrectAnswerFeedback)
  end appendChoiceQuestion

  private def appendFitBQuestion(
    i: Int,
    data: FillInTheBlankContent,
    competencies: Seq[String],
    doc: XWPFDocument
  ): Unit =
    implicit val table: XWPFTable = this.table(doc)

    // map blank pattern starts to which blank number it is
    val matches                              =
      FillInTheBlankQuestion.BlankPattern.findAllMatchIn(data.questionComplexText.html).zipWithIndex collectToMap {
        case (mtch, idx) => mtch.start -> (idx + 1)
      }
    def blankName(mtch: Regex.Match): String = if matches.size == 1 then "_" else s"_${matches(mtch.start)}_"
    val bowdlerized                          = FillInTheBlankQuestion.BlankPattern.replaceAllIn(
      data.questionComplexText.html,
      blankName
    )
    appendHeader(i, bowdlerized, FillInTheBlank)
    FillInTheBlankQuestion.BlankPattern.findAllMatchIn(data.questionComplexText.html) foreach { mtch =>
      mtch.group(1).split(";") foreach { opt =>
        appendTuple(Blank(blankName(mtch)), opt)
      }
    }

    (competencies || Seq("")) foreach { title =>
      appendTuple(Assesses, title)
    }
    FillInTheBlankQuestion.BlankPattern.findAllIn(data.questionComplexText.html)

    appendFooter(data.richCorrectAnswerFeedback, data.richIncorrectAnswerFeedback)
  end appendFitBQuestion

  private def appendFooter(correct: Option[HtmlPart], incorrect: Option[HtmlPart])(implicit table: XWPFTable): Unit =
    correct.filterNot(_.html.isBlank) foreach { html =>
      appendTuple(CorrectFeedback, html.html, html = true)
    }
    incorrect.filterNot(_.html.isBlank) foreach { html =>
      appendTuple(IncorrectFeedback, html.html, html = true)
    }

  private def appendHeader(
    i: Int,
    html: String,
    qt: DocxQuestionType
  )(implicit table: XWPFTable): Unit =
    val cells = table.getRows.get(0).getTableCells
    cells.get(0).setHtml(s"${i + 1}")
    cells.get(1).setHtml(html)
    cells.get(2).setHtml(qt.entryName)

  def appendTuple(
    key: DocxTuple,
    value: String,
    html: Boolean = false
  )(implicit table: XWPFTable): Unit =
    val cells = addRow(Seq(25, 75)).getTableCells
    cells.get(0).setText(key.entryName)
    if html then cells.get(1).setHtml(value)
    else cells.get(1).setText(value)

  private def table(doc: XWPFDocument) =
    val table = doc.createTable(1, 3)
    doc.createParagraph()                     //
    table.setCellMargins(90, 90, 90, 90)
    table.setWidth(s"${100 * ScalingFactor}") // sufficient for OpenOffice

    // MS-Word :/
    val grid = table.getCTTbl.addNewTblGrid()
    (1 to 100).foreach { _ =>
      grid.addNewGridCol().setW(BigInteger.valueOf(ScalingFactor))
    }

    table.getRow(0).getTableCells.asScala.zip(Seq(10, 70, 20)).foreach { case (c, w) =>
      c.setWidth(w)
    }
    table
  end table

  private def addRow(widths: Seq[Int])(implicit t: XWPFTable) =
    assert(widths.nonEmpty && widths.sum == 100)
    val row  = t.createRow()
    val diff = widths.size - row.getTableCells.size()
    if diff > 0 then (0 until diff).foreach(_ => row.addNewTableCell())
    else (diff until 0).foreach(_ => row.removeCell(0))
    widths.zipWithIndex.foreach { case (w, i) => row.getCell(i).setWidth(w) }
    row

  private implicit class CellOps(private val c: XWPFTableCell) extends AnyVal:
    def setWidth(w: Int): Unit =
      c.setWidth(s"${w * ScalingFactor}") // sufficient for OpenOffice
      c.getCTTc.getTcPr.addNewGridSpan().setVal(BigInteger.valueOf(w)) // MS-Word :/

    def setHtml(html: String): Unit = HtmlSink(c).addHtml(html)

    def setHtml(p: Option[HtmlPart]): Unit = p.map(_.html).foreach(c.setHtml)

  private[docx] val ScalingFactor = 90
end DocxQuestionAppender

private[exchange] sealed trait DocxTuple extends EnumEntry with CapitalWords

private[exchange] object DocxTuple extends Enum[DocxTuple]:

  override def values: IndexedSeq[DocxTuple] = findValues

  case object Assesses          extends DocxTuple
  case object CorrectFeedback   extends DocxTuple
  case object IncorrectFeedback extends DocxTuple

  final case class Blank(override val entryName: String) extends DocxTuple
