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
import loi.cp.submissionassessment.attempt.event.{AttemptReopenedEvent, SubmissionAttemptEvent}
import loi.cp.submissionassessment.attempt.{IllegalState, SubmissionAttempt, SubmissionAttemptFailure}
import scalaz.\/
import scalaz.syntax.either.*

/** Reopens a finalized attempt. This changes the state from [[loi.cp.assessment.attempt.AttemptState.Finalized]] to
  * [[loi.cp.assessment.attempt.AttemptState.Submitted]]. Attempting to reopen an
  * [[loi.cp.assessment.attempt.AttemptState.Open]] or [[loi.cp.assessment.attempt.AttemptState.Submitted]] is invalid.
  */
case class ReopenAttempt(scorer: Long) extends SubmissionAttemptAction:

  override def exec(params: SubmissionAttemptActionParameters): SubmissionAttemptFailure \/ SubmissionAttempt =
    if params.attempt.state == AttemptState.Finalized then params.attempt.copy(state = AttemptState.Submitted).right
    else IllegalState(s"Attempt is not finalized").left

  override def events(parameters: SubmissionAttemptActionParameters): Seq[SubmissionAttemptEvent] =
    Seq(AttemptReopenedEvent)
