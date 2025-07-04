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

package loi.cp.quiz.question.choice

import loi.asset.contentpart.HtmlPart
import loi.asset.question.ChoiceContent
import loi.cp.quiz.attempt.{DistractorIndexList, DistractorOrder}
import loi.cp.quiz.question.{CorrectRationale, RandomizableQuestion, Rationale, TextRemediation}

trait ChoiceQuestion extends RandomizableQuestion:
  def choices: Seq[Choice]
  override def generateDistractorOrder(): DistractorIndexList =
    DistractorIndexList(DistractorOrder.randomIndices(choices))

case class Choice(text: HtmlPart, correct: Boolean, points: Double, rationales: Seq[Rationale])

object ChoiceQuestion:
  def choice(content: ChoiceContent): Choice =
    val rationales: Seq[Rationale] = content.correctChoiceFeedback
      .flatMap(_.renderedHtml)
      .map(feedback => CorrectRationale(feedback))
      .toSeq ++
      content.incorrectChoiceFeedback.flatMap(_.renderedHtml).map(feedback => TextRemediation(feedback))

    Choice(content.description, content.correct, content.points, rationales)
