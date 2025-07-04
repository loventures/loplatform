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

package loi.cp.submissionassessment.attempt

import com.learningobjects.cpxp.Id
import loi.cp.Widen
import loi.cp.assessment.rubric.RubricScoringFailure
import loi.cp.submissionassessment.settings.Driver

/** Any sort of failing a submission attempt could have interacting with it
  */
sealed abstract class SubmissionAttemptFailure(val message: String) extends Widen[SubmissionAttemptFailure]

case class IllegalDriver(driver: Driver)
    extends SubmissionAttemptFailure(s"User is unable to interact with $driver Submission Assessment")

case class AttemptLimitExceeded(maxAttempts: Int)
    extends SubmissionAttemptFailure(s"No more submission attempts allowed. Max attempts = $maxAttempts")

case class AttemptNotFound(attemptId: Long)
    extends SubmissionAttemptFailure(s"Submission attempt $attemptId not found.")

case class IllegalResponseState(stateMessage: String) extends SubmissionAttemptFailure(stateMessage)

case class IllegalState(stateMessage: String) extends SubmissionAttemptFailure(stateMessage)

/** A user attempted to invalidate an already invalid attempt.
  *
  * @param attempt
  *   the attempt in question
  */
case class AlreadyInvalidFailure(attempt: Id)
    extends SubmissionAttemptFailure(
      s"Submission attempt ${attempt.getId} already invalidated. Cannot invalidate again."
    )

////////////////////////////////////////////////

/** Any sort of failing when scoring a response.
  */
sealed abstract class SubmissionAttemptScoringFailure(message: String) extends SubmissionAttemptFailure(message)

case class SubmissionRubricScoringFailure(rubricFailure: RubricScoringFailure)
    extends SubmissionAttemptScoringFailure(rubricFailure.message)
