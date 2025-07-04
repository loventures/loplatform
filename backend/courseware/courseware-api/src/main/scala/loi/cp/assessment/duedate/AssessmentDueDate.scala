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

package loi.cp.assessment.duedate

import java.time.Instant

/** A due date with the context of how the [[loi.cp.assessment.Assessment]] is supposed to behave when the due date has
  * pasted.
  */
abstract class AssessmentDueDate(date: Instant)

/** A due date that denotes a suggested time the subjects should finish the assessment by.
  *
  * @param date
  *   the recommended date for finishing the assessment
  */
case class AssessmentTargetDate(date: Instant) extends AssessmentDueDate(date)

/** A due date where learner actions are not allowed at or after.
  *
  * @param date
  *   the date where subjects will no longer be allowed to edit their attempt
  */
case class AssessmentDeadline(date: Instant) extends AssessmentDueDate(date)

/** A due date where a learner is exempt from due date policies
  *
  * @param date
  *   the date where non-exempt learners will no longer be allowed to edit their attempt
  */
case class AssessmentDeadlineExempted(date: Instant) extends AssessmentDueDate(date)
