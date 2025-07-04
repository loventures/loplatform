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

package loi.cp.quiz.attempt.actions

import loi.cp.assessment.BasicScore
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.score.*
import loi.cp.quiz.attempt.selection.*
import loi.cp.quiz.question.bindrop.BinDrop
import loi.cp.quiz.question.choice.{MultipleChoice, MultipleSelect, TrueFalse}
import loi.cp.quiz.question.fillintheblank.FillInTheBlank
import loi.cp.quiz.question.hotspot.Hotspot
import loi.cp.quiz.question.matching.Matching
import loi.cp.quiz.question.ordering.Sequencing
import loi.cp.quiz.question.shortanswer.ShortAnswer
import loi.cp.quiz.question.{AutoScorable, ManualGrading, Question}
import loi.cp.quiz.settings.{NavigationPolicy, Paged, SinglePage}
import scalaz.\/
import scalaz.syntax.either.*

object AttemptResponseUtils:
  def validateResponseIndex(questions: Seq[?], questionIndex: Int): QuestionIndexOutOfBounds \/ Unit =
    if questionIndex >= 0 && questionIndex < questions.size then ().right
    else QuestionIndexOutOfBounds(questionIndex, questions.size).left

  def validateResponseIsOpen(response: QuizQuestionResponse): ClosedResponseModification.type \/ Unit =
    if !response.state.open then ClosedResponseModification.left
    else ().right

  def validateResponseNavigationState(
    navigationPolicy: NavigationPolicy,
    responses: Seq[QuizQuestionResponse],
    questionIndex: Int
  ): NoSkippingAllowed.type \/ Unit =
    val hasUnrespondedSelections: Boolean = responses.slice(0, questionIndex).exists(_.state.open)

    navigationPolicy match
      case SinglePage                                  => ().right
      case Paged(false, _) if hasUnrespondedSelections => NoSkippingAllowed.left
      case _                                           => ().right
  end validateResponseNavigationState

  def validateSelection(
    question: Question,
    response: Option[QuestionResponseSelection]
  ): QuizAttemptSelectionFailure \/ Option[BasicScore] =
    scoreResponse(question, response)

  def scoreResponse(
    question: Question,
    maybeSelection: Option[QuestionResponseSelection]
  ): QuizAttemptSelectionFailure \/ Option[BasicScore] =
    (question, maybeSelection) match
      case (q: AutoScorable, Some(s: QuestionResponseSelection)) => calculateAutomaticScore(s, q).map(Some(_))
      case (_: ManualGrading, _)                                 => Option.empty[BasicScore].right
      case (q: Question, None)                                   => Option(ResponseScores.zero(q)).right

  def updateResponse(attempt: QuizAttempt, questionIndex: Int)(
    transform: QuizQuestionResponse => QuizQuestionResponse
  ): QuizAttempt =
    val originalResponse = attempt.responses(questionIndex)
    val updatedResponse  = transform(originalResponse)

    val (prev, next)                                = attempt.responses.splitAt(questionIndex)
    val updatedResponses: Seq[QuizQuestionResponse] = (prev :+ updatedResponse) ++ next.tail

    attempt.copy(responses = updatedResponses)
  end updateResponse

  private def calculateAutomaticScore(
    selection: QuestionResponseSelection,
    question: Question
  ): QuizAttemptSelectionFailure \/ BasicScore =
    (question, selection) match
      case (mc: MultipleChoice, s: ChoiceSelection)         =>
        MultipleChoiceResponseScorer.score(s, mc)
      case (sa: ShortAnswer, s: FreeTextSelection)          =>
        ShortAnswerResponseScorer.score(s, sa).right
      case (h: Hotspot, s: HotspotSelection)                =>
        HotspotResponseScorer.score(s, h).right
      case (ms: MultipleSelect, s: ChoiceSelection)         =>
        MultipleSelectResponseScorer.score(s, ms)
      case (bd: BinDrop, s: GroupingSelection)              =>
        BinDropResponseScorer.score(s, bd)
      case (fitb: FillInTheBlank, s: BlankEntriesSelection) =>
        FillInTheBlankResponseScorer.score(s, fitb)
      case (o: Sequencing, s: OrderingSelection)            =>
        SequencingResponseScorer.score(s, o)
      case (m: Matching, s: GroupingSelection)              =>
        MatchingQuestionResponseScorer.score(s, m)
      case (tf: TrueFalse, s: ChoiceSelection)              =>
        TrueFalseResponseScorer.score(s, tf)
      case _                                                =>
        MismatchedResponseType(question, selection).left[BasicScore]
end AttemptResponseUtils
