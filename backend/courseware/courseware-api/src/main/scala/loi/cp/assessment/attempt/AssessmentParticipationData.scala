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

import java.time.Instant

import loi.cp.reference.ContentIdentifier

/** An aggregation of attempts against a specified assessment.
  *
  * @param identifier
  *   The assessment identifier.
  * @param validAttempts
  *   The total number of valid attempts against the assessment.
  * @param awaitingInstructorInput
  *   The attempts that are in submitted state, awaiting input from the instructor.
  * @param participantCount
  *   The number of unique students who have valid attempts against the assessment.
  * @param latestUpdate
  *   The latest update time across all attempts against the assessment.
  */
case class AssessmentParticipationData(
  identifier: ContentIdentifier,
  validAttempts: Int,
  awaitingInstructorInput: Int,
  participantCount: Int,
  latestUpdate: Option[Instant]
)
