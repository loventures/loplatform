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
import loi.asset.contentpart.HtmlPart
import loi.asset.question.*
import loi.authoring.exchange.docx.DocxErrors.*
import loi.authoring.exchange.docx.*
import loi.authoring.write.{AddNode, WriteOp}
import scalaz.Validation.FlatMap.*
import scalaz.syntax.apply.*

import java.util.UUID
import scala.collection.BitSet

trait MultipleChoiceToWriteOps extends TableToWriteOps:
  import TableToWriteOps.*

  override def writes(table: Table, competencies: Map[String, UUID]): ValNel[Warned[List[WriteOp]]] =
    val data = questionData(table)
    for
      _          <- (needXRows(table, 3) |@|
                      maxChoices(table.idx + 1, 10, data.choiceRows.size))((_, _) => ())
      cs         <- validateChoices(table, data.choiceRows)(choice)
      _          <- countValNel(cs, _ == 1)(_.isCorrect, RequiresOneCorrectError(data.number))
      (w0, _)     = validateTupleRows(table)
      (w1, comps) = validateCompetencies(data, competencies)
    yield
      val node = AddNode(
        MultipleChoiceQuestion(
          title = data.plainText,
          questionContent = ChoiceQuestionContent(
            questionText = Some(data.plainText),
            questionComplexText = HtmlPart(data.html),
            choices = cs.zipWithIndex.map { case (c, i) =>
              ChoiceContent(
                index = i,
                correct = c.isCorrect,
                points = if c.isCorrect then 1.0 else 0.0,
                choiceText = Some(toPlaintext(c.html.html)),
                choiceContent = Some(c.html),
                correctChoiceFeedback = c.feedback.filter(_ => c.isCorrect),
                incorrectChoiceFeedback = c.feedback.filter(_ => !c.isCorrect)
              )
            },
            richCorrectAnswerFeedback = data.correctFeedback.map(HtmlPart(_)).orElse(OddDefaultFeedback),
            richIncorrectAnswerFeedback = data.incorrectFeedback.map(HtmlPart(_)).orElse(OddDefaultFeedback),
          )
        )
      )
      (w0 ::: w1, node :: competencyWriteOps(node.name, comps))
    end for
  end writes

  private def choice(table: Table, i: Int): ValNel[Choice] =
    cols(table, i, 3, BitSet(1)).map {
      case List(c0, c1, c2) =>
        Choice(
          html = HtmlPart(c1.text()),
          isCorrect = isCorrectChoice(c0),
          feedback = feedback(c2)
        )
      case _                => ??? // can't happen
    }

  private case class Choice(html: HtmlPart, isCorrect: Boolean, feedback: Option[HtmlPart])
end MultipleChoiceToWriteOps
