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

package loi.cp.quiz.attempt.score

import loi.cp.assessment.BasicScore
import loi.cp.quiz.attempt.selection.BlankEntriesSelection
import loi.cp.quiz.attempt.{InvalidSelectionIndex, ResponseScores}
import loi.cp.quiz.question.fillintheblank.FillInTheBlank
import scalaz.\/
import scalaz.syntax.either.*

object FillInTheBlankResponseScorer:
  def score(
    selection: BlankEntriesSelection,
    question: FillInTheBlank
  ): InvalidSelectionIndex.type \/ BasicScore =
    // Based on BinDropQuestionResponseGrader
    for validSelection <- validate(selection, question)
    yield
      val isResponseCorrect: Seq[Boolean] = areResponsesCorrect(question, validSelection)

      if question.awardsPartialCredit then
        val numberCorrect: Int     =
          isResponseCorrect
            .count(_ == true)
        val percentCorrect: Double = numberCorrect.toDouble / question.blanks.size.toDouble
        ResponseScores.of(percentCorrect * question.pointValue, question)
      else if isResponseCorrect.forall(_ == true) then
        // All blanks are correct
        ResponseScores.allOf(question)
      else
        // At least one wrong means you get the whole question wrong
        ResponseScores.zero(question)
      end if

  def areResponsesCorrect(question: FillInTheBlank, validSelection: BlankEntriesSelection): Seq[Boolean] =
    // Both the correct answer and the selection are in /response/ order at this point
    val responses: Seq[String] = validSelection.entries

    // Check each and every blank to see if we have a correct response for it
    val isResponseCorrect: Seq[Boolean] =
      for (index, blank) <- question.blanks.indices zip question.blanks
      yield
        if index >= responses.size || (responses(index) eq null) then false // No response provided
        else
          val response = normaliseWhitespace(responses(index))
          blank.answers.exists(answer =>
            if question.caseSensitive then response `equals` normaliseWhitespace(answer)
            else response `equalsIgnoreCase` normaliseWhitespace(answer)
          )
    isResponseCorrect
  end areResponsesCorrect

  private def normaliseWhitespace(s: String): String = s.trim.replaceAll("\\s+", " ")

  private def validate(
    selection: BlankEntriesSelection,
    question: FillInTheBlank
  ): InvalidSelectionIndex.type \/ BlankEntriesSelection =
    if selection.entries.size <= question.blanks.size then selection.right
    else InvalidSelectionIndex.left
end FillInTheBlankResponseScorer
