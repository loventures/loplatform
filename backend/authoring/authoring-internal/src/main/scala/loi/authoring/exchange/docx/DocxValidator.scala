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

import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.authoring.asset.Asset

import java.io.InputStream
import loi.authoring.exchange.docx.DocxErrors.{DocxValidationError as Err, *}
import loi.authoring.write.{SetNodeData, WriteOp}
import org.apache.poi.xwpf.usermodel.*
import scala.util.Using
import scalaz.\/
import scalaz.Validation.FlatMap.*
import scalaz.std.list.*
import scalaz.std.tuple.*
import scalaz.syntax.apply.*
import scaloi.syntax.boolean.*
import scalaz.syntax.nel.*
import scalaz.syntax.traverse.*
import scaloi.syntax.validation.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.util.UUID
import scala.collection.BitSet
import scala.jdk.CollectionConverters.*
import scala.util.Try
import loi.asset.util.Assex.*
import loi.authoring.exchange.docx.format.DocxFormat.html
import loi.authoring.syntax.index.*

private[docx] trait DocxValidator:

  final def toWriteOps(
    in: InputStream,
    assessment: Option[Asset[?]],
    competencies: Map[String, UUID]
  ): ValNel[Warned[List[WriteOp]]] =
    for
      xDoc                  <- xwpfDoc(in)
      xTables                = xDoc.getTables.asScala.toList
      instructionOp          = assessment.flatMap(instructionsOp(_, xDoc))
      (warnings, qWriteOps) <- writeOps(xTables, competencies)
    yield warnings -> (instructionOp ::? qWriteOps)

  // If the current asset instructions are blank then replace them with the leading
  // paragraphs from the doc save the first one that is the assessment title. Probably.
  private def instructionsOp[A](assessment: Asset[A], doc: XWPFDocument): Option[WriteOp] =
    assessment.instructions.strings.forall(_.isBlank) flatOption {
      val instructions = html(leadingParagraphs(doc).drop(1))
      assessment
        .withInstructions(BlockPart(Seq(HtmlPart(instructions))))
        .map(SetNodeData.fromAsset)
    }

  private def leadingParagraphs(doc: XWPFDocument): List[XWPFParagraph] =
    doc.getBodyElementsIterator.asScala.toList collectWhile { case para: XWPFParagraph => para }

  final protected def xwpfDoc(in: InputStream): ValNel[XWPFDocument] =
    Using.resource(in) { is =>
      \/.attempt(
        new XWPFDocument(is)
      ) { ex =>
        log.warn(ex)(s"while opening input stream")
        val err: Err = NoQuestionsError
        err.wrapNel
      }.toValidation
    }

  final protected def writeOps(
    xTables: List[XWPFTable],
    competencies: Map[String, UUID]
  ): ValNel[Warned[List[WriteOp]]] =
    val tables = xTables.zipWithIndex.map { case (t, i) => Table(t, i) }
    val qTypes = tables.map(questionType)
    val result = qTypes.zipWithIndex traverse { case (qtVal, i) =>
      for
        qType   <- qtVal
        qWrites <- qType.writes(tables(i), competencies)
      yield qWrites
    }
    result.map(questionsOps => questionsOps.suml)
  end writeOps

  private def questionType(table: Table): ValNel[DocxQuestionType] =
    val qNum = table.idx + 1
    cols(table, 0, 3, BitSet(0, 1, 2)).flatMap {
      case List(n, q, t) =>
        (correctNum(n.text(false), qNum, q.text(false)) |@| knownType(qNum, t.text(false)))((a, b) => (a, b))
          .map { case (_, qType) => qType }
      case _             => ??? // can't happen
    }

  private def knownType(qNum: Int, tText: String) =
    DocxQuestionType
      .withNameInsensitiveOption(tText)
      .validNelWhen[Err](
        _.isDefined,
        UnknownQuestionTypeError(qNum, tText)
      )
      .map(_.get)

  private def correctNum(actual: String, expected: Int, qText: String) =
    expected.validNelWhen[Err](
      _ == Try(actual.toInt).getOrElse(-1),
      InvalidQuestionNumberError(expected, qText)
    )

  private val log = org.log4s.getLogger
end DocxValidator

private[exchange] object DocxValidator extends DocxValidator
