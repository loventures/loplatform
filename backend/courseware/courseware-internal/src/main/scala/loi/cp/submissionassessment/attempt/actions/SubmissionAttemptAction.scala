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

import loi.cp.submissionassessment.SubmissionAssessment
import loi.cp.submissionassessment.attempt.event.SubmissionAttemptEvent
import loi.cp.submissionassessment.attempt.{SubmissionAttempt, SubmissionAttemptFailure}
import scalaz.\/

import java.time.Instant

/** An action that may be performed against an attempt, returning the attempt and an indication of failure or success.
  */
trait SubmissionAttemptAction:

  /** Executes this action and returns the state of the resulting attempt. This does not persist any effects applied to
    * the attempt.
    *
    * @param parameters
    *   the parameters for this action
    * @return
    *   the updated attempt
    */
  def exec(parameters: SubmissionAttemptActionParameters): SubmissionAttemptFailure \/ SubmissionAttempt

  /** Returns all events that should be raised when this action executes successfully.
    *
    * @param parameters
    *   the parameters for this action
    * @return
    *   all events spawned from this action
    */
  def events(parameters: SubmissionAttemptActionParameters): Seq[SubmissionAttemptEvent]
end SubmissionAttemptAction

case class SubmissionAttemptActionParameters(
  attempt: SubmissionAttempt,
  assessment: SubmissionAssessment,
  time: Instant
)

object SubmissionAttemptActionParameters:
  def params(
    attempt: SubmissionAttempt,
    assessment: SubmissionAssessment,
    time: Instant
  ): SubmissionAttemptActionParameters =
    SubmissionAttemptActionParameters(attempt, assessment, time)
