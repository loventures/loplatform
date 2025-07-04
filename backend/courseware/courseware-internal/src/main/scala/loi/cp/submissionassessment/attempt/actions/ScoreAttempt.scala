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

package loi.cp.submissionassessment.attempt.actions

import loi.cp.assessment.ResponseScore
import loi.cp.assessment.attempt.AttemptState
import loi.cp.assessment.rubric.RubricScoreValidationUtils.validateScore
import loi.cp.submissionassessment.attempt.*
import loi.cp.submissionassessment.attempt.event.SubmissionAttemptEvent
import scalaz.\/
import scalaz.syntax.either.*

case class ScoreAttempt(score: Option[ResponseScore], scorer: Long) extends SubmissionAttemptAction:

  override def exec(parameters: SubmissionAttemptActionParameters): SubmissionAttemptFailure \/ SubmissionAttempt =
    if parameters.attempt.state == AttemptState.Submitted then
      // The _unvalidated_ updated attempt
      val updatedAttempt: SubmissionAttempt =
        parameters.attempt.copy(score = score, scorer = Some(scorer), scoreTime = Some(parameters.time))

      validateScore(parameters.assessment.rubric, score, submitResponse = false)
        .leftMap(f => SubmissionRubricScoringFailure(f))
        .map(_ => updatedAttempt)
    else IllegalResponseState("Cannot respond to a non-submitted attempt").left

  override def events(parameters: SubmissionAttemptActionParameters): Seq[SubmissionAttemptEvent] = Nil
end ScoreAttempt
