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

package loi.cp.quiz.attempt.event

import loi.cp.assessment.ResponseScore
import loi.cp.quiz.attempt.selection.QuestionResponseSelection

/** An event that should signal a condition to an external system. These events are created when certain actions are
  * successfully executed.
  */
sealed trait QuizAttemptEvent

/** An event to capture a question response scoring action. This will capture the response index.
  *
  * @param responseIndex
  *   the index of the question being responded to.
  */
case class SaveResponseEvent(responseIndex: Int, selection: Option[QuestionResponseSelection]) extends QuizAttemptEvent

/** Event capturing a question response score.
  *
  * @param responseIndex
  *   The question response index.
  * @param score
  *   The response score.
  */
case class ScoreResponseEvent(responseIndex: Int, score: Option[ResponseScore]) extends QuizAttemptEvent

/** An event for when an attempt is submitted and closed for user input.
  * @param andFinalized
  *   true if AttemptFinalizedEvent occurs in this QuizAttemptAction performance, false otherwise.
  */
case class AttemptSubmittedEvent(andFinalized: Boolean) extends QuizAttemptEvent

/** An event for when an attempt is finalized. After an attempt is finalized, no changes to the responses or scores may
  * be made.
  * @param andSubmitted
  *   true if AttemptSubmittedEvent occurs in this QuizAttemptAction performance, false otherwise.
  */
case class AttemptFinalizedEvent(andSubmitted: Boolean) extends QuizAttemptEvent
