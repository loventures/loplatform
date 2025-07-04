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

package loi.cp.assessment.attempt

import enumeratum.{Enum, EnumEntry}

/** An object representing the current state of an attempt. This signals whether the user or score may act upon the
  * attempt.
  */
sealed abstract class AttemptState extends EnumEntry

case object AttemptState extends Enum[AttemptState]:
  val values = findValues

  /** The attempt is open and attempt may be responded to. */
  case object Open extends AttemptState

  /** The user has completed their attempt, and the attempt is ready for grading. */
  case object Submitted extends AttemptState

  /** A finalized score has been assigned to this attempt. External systems have been informed about this attempt. */
  case object Finalized extends AttemptState
end AttemptState
