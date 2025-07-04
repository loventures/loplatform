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

package loi.cp.submissionassessment.attempt.event

import loi.cp.attachment.AttachmentId

/** An event that should signal a condition to an external system. These events are created when certain actions are
  * successfully executed.
  */
sealed trait SubmissionAttemptEvent

/** An event to capture a response action.
  *
  * @param essay
  *   an essay, if any, for the response
  * @param attachments
  *   any attachments for the response
  */
case class SaveResponseEvent(essay: Option[String], attachments: Seq[AttachmentId]) extends SubmissionAttemptEvent

/** An event for when an attempt is submitted and closed for driver input.
  * @param andFinalized
  *   true if AttemptFinalizedEvent occurs in this SubmissionAttemptAction performance, false otherwise.
  */
case class AttemptSubmittedEvent(andFinalized: Boolean) extends SubmissionAttemptEvent

/** An event for when an attempt is invalidated. Invalidated attempts do not get counted in most processes.
  */
case object AttemptInvalidatedEvent extends SubmissionAttemptEvent

/** An event for when an attempt is finalized. After an attempt is finalized, no changes to the response or score may be
  * made.
  * @param andSubmitted
  *   true if AttemptSubmittedEvent occurs in this SubmissionAttemptAction performance, false otherwise.
  */
case class AttemptFinalizedEvent(andSubmitted: Boolean) extends SubmissionAttemptEvent

/** An event for when an attempt is reopened. Reopening an attempt moves the attempt out of the finalized state. Any
  * external system must account for the fact the score may now be changed on the attempt.
  */
case object AttemptReopenedEvent extends SubmissionAttemptEvent
