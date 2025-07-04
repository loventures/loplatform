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

import loi.asset.contentpart.HtmlPart
import loi.asset.question.*
import loi.authoring.exchange.docx.DocxErrors.{
  DocxValidationError,
  InvalidBlankError,
  MissingBlankError,
  UnknownBlankError
}
import loi.authoring.exchange.docx.*
import loi.authoring.write.{AddNode, WriteOp}
import scalaz.Validation.FlatMap.*
import scalaz.std.list.*
import scalaz.syntax.apply.*
import scalaz.syntax.traverse.*
import scaloi.syntax.boolean.*

import java.util.UUID

trait FillInTheBlankWriteOps extends TableToWriteOps:

  import TableToWriteOps.*

  override def writes(table: Table, competencies: Map[String, UUID]): ValNel[Warned[List[WriteOp]]] =
    val number         = table.idx + 1
    val data           = questionData(table)
    val questionBlanks = BlankRE.findAllIn(data.plaintext).toList

    for
      _           <- (needXRows(table, 3) |@|
                       maxChoices(number, 10, data.choiceRows.size))((_, _) => ())
      (w0, tuples) = validateTupleRows(table, blanks = true)
      blankAnswers = tuples.collect { case (DocxTuple.Blank(blank), cells) =>
                       blank -> cells.map(_.text(formatted = false))
                     }
      _           <- questionBlanks.traverse(validateQuestionBlank(number, _, blankAnswers))
      _           <- blankAnswers.toList.traverse(t => validateBlankAnswers(number, t._1, t._2, questionBlanks))
      (w1, comps)  = validateCompetencies(data, competencies)
    yield
      val rewritten =
        BlankRE.replaceAllIn(data.plaintext, mtch => blankAnswers(mtch.group(0)).mkString("{{", ";", "}}"))
      val node      = AddNode(
        FillInTheBlankQuestion(
          title = rewritten,
          questionContent = FillInTheBlankContent(
            questionText = Some(rewritten),
            questionComplexText = HtmlPart(rewritten),
            richCorrectAnswerFeedback = data.correctFeedback.map(HtmlPart(_)).orElse(OddDefaultFeedback),
            richIncorrectAnswerFeedback = data.incorrectFeedback.map(HtmlPart(_)).orElse(OddDefaultFeedback),
          )
        )
      )
      (w0 ::: w1, node :: competencyWriteOps(node.name, comps))
    end for
  end writes

  private def validateQuestionBlank(number: Int, blank: String, answers: Map[String, List[String]]): ValNel[Unit] =
    answers.contains(blank).elseInvalidNel(MissingBlankError(number, blank))

  private def validateBlankAnswers(
    number: Int,
    blank: String,
    answers: List[String],
    questionBlanks: List[String]
  ): ValNel[Unit] =
    for
      _ <- questionBlanks.contains(blank).elseInvalidNel[DocxValidationError](UnknownBlankError(number, blank))
      _ <- answers.traverse(_.isBlank.thenInvalidNel[DocxValidationError](InvalidBlankError(number, blank)))
    yield ()

  private final val BlankRE = "_[_a-zA-Z0-9]*".r
end FillInTheBlankWriteOps
