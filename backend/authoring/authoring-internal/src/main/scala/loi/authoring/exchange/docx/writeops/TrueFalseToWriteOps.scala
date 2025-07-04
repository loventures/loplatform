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
package writeops

import loi.asset.contentpart.HtmlPart
import loi.asset.question.*
import loi.authoring.exchange.docx.DocxErrors.*
import loi.authoring.write.{AddNode, WriteOp}
import scalaz.Validation.FlatMap.*
import scalaz.syntax.apply.*

import java.util.UUID
import scala.collection.BitSet

private[docx] trait TrueFalseToWriteOps extends TableToWriteOps:
  import TableToWriteOps.*

  override def writes(table: Table, competencies: Map[String, UUID]): ValNel[Warned[List[WriteOp]]] =
    val data = questionData(table)
    for
      _          <- (needXRows(table, 3) |@|
                      maxChoices(table.idx + 1, 2, data.choiceRows.size))((_, _) => ())
      cs         <- validateChoices(table, data.choiceRows)(choice)
      onlyOne     = countValNel(cs, _ == 1)(_, _)
      _          <- (onlyOne(_.isCorrect, RequiresOneCorrectError(data.number)) |@|
                      onlyOne(_.isTrue, RequiresOneTrueError(data.number)))((_, _) => ())
      (w0, _)     = validateTupleRows(table)
      (w1, comps) = validateCompetencies(data, competencies)
    yield
      val node = AddNode(
        TrueFalseQuestion(
          title = data.plainText,
          questionContent = ChoiceQuestionContent(
            questionText = Some(data.plainText),
            questionComplexText = HtmlPart(data.html),
            choices = cs.zipWithIndex.map { case (c, i) =>
              val html = HtmlPart(if c.isTrue then "true" else "false")
              ChoiceContent(
                index = i,
                correct = c.isCorrect,
                points = if c.isCorrect then 1.0 else 0.0,
                choiceText = Some(html.html),
                choiceContent = Some(html),
                correctChoiceFeedback = c.feedback.filter(_ => c.isCorrect),
                incorrectChoiceFeedback = c.feedback.filter(_ => !c.isCorrect)
              )
            },
            richCorrectAnswerFeedback = data.correctFeedback.map(HtmlPart(_)).orElse(OddDefaultFeedback),
            richIncorrectAnswerFeedback = data.incorrectFeedback.map(HtmlPart(_)).orElse(OddDefaultFeedback),
            allowDistractorRandomization = Some(false),
          )
        )
      )
      (w0 ::: w1, node :: competencyWriteOps(node.name, comps))
    end for
  end writes

  private def choice(table: Table, i: Int) =
    cols(table, i, 3, BitSet(1)).map {
      case List(c0, c1, c2) =>
        TFChoice(
          isCorrect = isCorrectChoice(c0),
          isTrue = c1.text(formatted = false).toLowerCase.startsWith("t"),
          feedback = feedback(c2)
        )
      case _                => ??? // can't happen
    }

  private case class TFChoice(isCorrect: Boolean, isTrue: Boolean, feedback: Option[HtmlPart])
end TrueFalseToWriteOps
