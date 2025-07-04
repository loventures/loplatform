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

package loi.cp.quiz.attempt

import enumeratum.{Enum, EnumEntry}

/** An object representing the state of a response, dictating what actions and views are available for the response.
  */
sealed abstract class QuestionResponseState extends EnumEntry:

  /** Whether or not the subject may alter the response */
  val open: Boolean

  /** Whether or not the subject has completed this response (alternately whether the response is available for scoring)
    */
  val scoreSubmitted: Boolean

  /** Whether the score is available to the subject or not */
  val scoreReleased: Boolean
end QuestionResponseState

object QuestionResponseState extends Enum[QuestionResponseState]:
  val values = findValues

  /** A state where the subject is still answering the response. Responses may be altered by the subject and scoring is
    * not allowed.
    */
  case object NotSubmitted extends QuestionResponseState:
    override val open: Boolean           = true
    override val scoreSubmitted: Boolean = false
    override val scoreReleased: Boolean  = false

  /** A state where the subject has completed their response and it awaits grading. Responses may not be altered and are
    * allowed to be scored.
    */
  case object ResponseSubmitted extends QuestionResponseState:
    override val open: Boolean           = false
    override val scoreSubmitted: Boolean = false
    override val scoreReleased: Boolean  = false

  /** A state where the scorer has provided a score to the response, but it is not available to the subject yet.
    */
  case object ResponseScored extends QuestionResponseState:
    override val open: Boolean           = false
    override val scoreSubmitted: Boolean = true
    override val scoreReleased: Boolean  = false

  /** A state where the scorer has provided a score to the response, and it is available to the subject.
    */
  case object ResponseScoreReleased extends QuestionResponseState:
    override val open: Boolean           = false
    override val scoreSubmitted: Boolean = true
    override val scoreReleased: Boolean  = true
end QuestionResponseState
