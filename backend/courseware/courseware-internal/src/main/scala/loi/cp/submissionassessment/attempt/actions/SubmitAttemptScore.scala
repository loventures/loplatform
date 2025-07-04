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

import loi.cp.assessment.attempt.AttemptState
import loi.cp.assessment.rubric.RubricScoreValidationUtils.validateScore
import loi.cp.submissionassessment.attempt.*
import loi.cp.submissionassessment.attempt.event.{AttemptFinalizedEvent, SubmissionAttemptEvent}
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*

case class SubmitAttemptScore() extends SubmissionAttemptAction:

  override def exec(parameters: SubmissionAttemptActionParameters): SubmissionAttemptFailure \/ SubmissionAttempt =
    if parameters.attempt.state == AttemptState.Submitted then
      for
        // important because a draft score may omit some rubric criteria scores but a submitted one may not
        _ <- validateScore(parameters.assessment.rubric, parameters.attempt.score, submitResponse = true)
               .leftMap(e => SubmissionRubricScoringFailure(e))
               .widenl
        _ <- parameters.attempt.score
               .toRightDisjunction(IllegalResponseState("Cannot submit attempt score without score"))
               .widenl
      yield parameters.attempt.copy(state = AttemptState.Finalized)
    else IllegalResponseState("Cannot score non-submitted attempt").left

  override def events(parameters: SubmissionAttemptActionParameters): Seq[SubmissionAttemptEvent] =
    Seq(AttemptFinalizedEvent(false))
end SubmitAttemptScore
