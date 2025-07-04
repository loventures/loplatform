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
import loi.cp.quiz.attempt.DistractorOrder.ResponseIndex
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.exceptions.MismatchedDistractorOrderException
import loi.cp.quiz.attempt.selection.GroupingSelection
import loi.cp.quiz.question.matching.{Definition, Matching, Term}
import scalaz.\/

object MatchingQuestionResponseScorer:
  type TermIndex       = Int
  type DefinitionIndex = Int

  def score(selection: GroupingSelection, question: Matching): QuizAttemptSelectionFailure \/ BasicScore =
    // FIXME this is hardcoded but shouldn't be
    val order: MatchingDistractorOrder = MatchingDistractorOrder(question.terms.indices, question.definitions.indices)
    val correctDefinitionByTerm        = correctAnswersInResponseOrder(question, order)
    for validSelection <- GroupingSelectionValidator.validate(
                            selection,
                            maxAllowedTermIndex = question.terms.size,
                            maxAllowedDefIndex = question.definitions.size,
                            allowMultipleDefsPerTerm = false
                          )
    yield
      // Both the correct answer and the selection are in /response/ order at this point
      val correctTermCount: Int =
        correctDefinitionByTerm
          .map({ case (termIndex, correctDefinitionIndex) =>
            val selectedDefinitionsForBin: Seq[DefinitionIndex] =
              validSelection.elementIndexesByGroupIndex.getOrElse(termIndex, Nil)

            val isEmptyAndCorrect: Boolean   = correctDefinitionIndex.isEmpty && selectedDefinitionsForBin.isEmpty
            val hasCorrectSelection: Boolean =
              selectedDefinitionsForBin.size == 1 && selectedDefinitionsForBin.head == correctDefinitionIndex.get
            isEmptyAndCorrect || hasCorrectSelection
          })
          .count(_ == true)

      val termCount: Int = question.terms.size

      val percentCorrect: Double = correctTermCount.toDouble / termCount.toDouble
      val pointsAwarded: Double  = percentCorrect * question.pointValue
      if question.awardsPartialCredit then ResponseScores.of(pointsAwarded, question)
      else ResponseScores.allOrNothingOf(pointsAwarded, question)
    end for
  end score

  private def correctAnswersInResponseOrder(
    question: Matching,
    order: MatchingDistractorOrder
  ): Map[TermIndex, Option[DefinitionIndex]] =
    val responseOrderDefinitions: Seq[Definition] = order.definitionsInResponseOrder(question.definitions)
    val responseOrderTerms: Seq[Term]             = order.termsInResponseOrder(question.terms)
    if responseOrderTerms.size == question.terms.size && responseOrderDefinitions.size == question.definitions.size then
      // Convert the authored order answers using the translations we created
      val correctMappingInResponseOrder: Map[TermIndex, Option[DefinitionIndex]] =
        (question.terms.zipWithIndex map { case (_, authoredIndex) =>
          val responseOrderTermIndex                            = order.authoredTermMap(authoredIndex)
          val maybeResponseOrderDefIndex: Option[ResponseIndex] =
            question.correctDefinitionForTerm.get(authoredIndex).map(order.authoredDefinitionMap)
          responseOrderTermIndex -> maybeResponseOrderDefIndex
        }).toMap

      correctMappingInResponseOrder
    else
      // Something went seriously wrong if we somehow are giving a distractor order that is not aligning with a question
      throw new MismatchedDistractorOrderException(question, order)
    end if
  end correctAnswersInResponseOrder
end MatchingQuestionResponseScorer
