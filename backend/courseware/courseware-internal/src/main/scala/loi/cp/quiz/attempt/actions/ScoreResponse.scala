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

import loi.cp.assessment.ResponseScore
import loi.cp.assessment.rubric.RubricScoreValidationUtils
import loi.cp.quiz.Quiz
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.event.{QuizAttemptEvent, ScoreResponseEvent}
import loi.cp.quiz.question.essay.Essay
import loi.cp.quiz.settings.ResultReleaseTime
import scalaz.\/
import scaloi.syntax.boolean.*

sealed abstract class ScoreResponse(
  questionIndex: Int,
  score: Option[ResponseScore],
  scorer: Option[Long],
  submitResponse: Boolean
) extends QuizAttemptAction:

  override def exec(params: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    for
      _       <- params.attempt.valid.elseLeft(InvalidatedAttemptScoringFailure).widenl
      _       <- AttemptResponseUtils.validateResponseIndex(params.attemptQuestions, questionIndex).widenl
      question = params.attemptQuestions(questionIndex)
      response = params.attempt.responses(questionIndex)
      rubric   = PartialFunction.condOpt(question)({ case e: Essay => e.rubric }).flatten
      _       <- RubricScoreValidationUtils
                   .validateScore(rubric, score, submitResponse)
                   .leftMap(QuizRubricScoringFailure.apply)
                   .widenl
    yield
      val updatedResponse = response.copy(
        score = score,
        scoreTime = Some(params.time),
        scorer = scorer,
        state = if submitResponse then scoreReleaseState(params.quiz) else response.state
      )

      params.attempt.copy(
        responses = params.attempt.responses.updated(questionIndex, updatedResponse),
        updateTime = params.time
      )

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] =
    if submitResponse then Seq(ScoreResponseEvent(questionIndex, params.attempt.responses(questionIndex).score))
    else Nil

  private def scoreReleaseState(quiz: Quiz): QuestionResponseState =
    if quiz.settings.resultsPolicy.resultReleaseTime == ResultReleaseTime.OnResponseScore then
      QuestionResponseState.ResponseScoreReleased
    else QuestionResponseState.ResponseScored
end ScoreResponse

/** Save a draft of a score for a specified question response. This does not require a fully-scored rubric if the
  * response score contains a rubric.
  *
  * @param questionIndex
  *   The question response index.
  * @param score
  *   The score that will be saved. This does not need to be a fully scored rubric.
  * @param scorer
  *   The ID of the user scoring this response.
  */
case class DraftResponseScore(questionIndex: Int, score: Option[ResponseScore], scorer: Option[Long])
    extends ScoreResponse(questionIndex, score, scorer, false)

/** Submit a score for a specified question response. If the score is a rubric response score, the sections must be
  * fully specified.
  *
  * @param questionIndex
  *   The question response index.
  * @param score
  *   The score that will be submitted. A rubric must have all sections specified.
  * @param scorer
  *   The ID of the user scoring this response.
  */
case class SubmitResponseScore(questionIndex: Int, score: ResponseScore, scorer: Option[Long])
    extends ScoreResponse(questionIndex, Some(score), scorer, true)
