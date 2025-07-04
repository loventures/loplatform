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
import loi.cp.attachment.AttachmentId
import loi.cp.submissionassessment.attempt.event.{SaveResponseEvent, SubmissionAttemptEvent}
import loi.cp.submissionassessment.attempt.{IllegalResponseState, SubmissionAttempt, SubmissionAttemptFailure}
import scalaz.\/
import scalaz.syntax.either.*

/** An action to update the essay and attachments for a [[SubmissionAttempt]]. This overwrites any existing data.
  */
case class Respond(essay: Option[String], attachments: Seq[AttachmentId]) extends SubmissionAttemptAction:

  override def exec(parameters: SubmissionAttemptActionParameters): SubmissionAttemptFailure \/ SubmissionAttempt =
    if parameters.attempt.state == AttemptState.Open then
      parameters.attempt.copy(essay = essay, attachments = attachments, responseTime = Some(parameters.time)).right
    else IllegalResponseState("Cannot respond to a non-open attempt").left

  override def events(parameters: SubmissionAttemptActionParameters): Seq[SubmissionAttemptEvent] =
    Seq(SaveResponseEvent(essay, attachments))
