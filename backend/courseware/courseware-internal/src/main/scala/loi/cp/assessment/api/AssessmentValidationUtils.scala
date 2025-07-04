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

package loi.cp.assessment.api
import java.time.Instant

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.assessment.Assessment
import loi.cp.assessment.duedate.{AssessmentDueDate, *}
import loi.cp.course.CourseSection
import scalaz.\/
import scalaz.syntax.either.*

/** Common validations for [[loi.cp.assessment.Assessment]] s.
  */
@Service
class AssessmentValidationUtils(dueDateService: AssessmentDueDateService):

  /** Returns the assessment due date if the learner has not passed the deadline
    *
    * @param section
    *   the section from the request
    * @param assessment
    *   the assessment to get the due date from
    * @param now
    *   the datetime to check validation with
    * @param learnerId
    *   the learner to check validation with
    * @return
    *   either the assessment due date or string reason validation failed
    */
  def validatedDueDate(
    section: CourseSection,
    assessment: Assessment,
    now: Instant,
    learnerId: Long
  ): String \/ Option[AssessmentDueDate] =
    val dueDate: Option[AssessmentDueDate] = dueDateService.getDueDate(section, assessment, learnerId)

    dueDate match
      case Some(AssessmentDeadline(deadline)) if now.isAfter(deadline) =>
        "Due date for assessment has passed".left
      case o                                                           =>
        o.right
  end validatedDueDate

  /** Returns if the learner has passed the deadline for a particular assessment
    *
    * @param now
    *   the datetime to check for
    * @return
    *   Returns if the learner has passed the deadline
    */
  def isPastDeadline(section: CourseSection, assessment: Assessment, now: Instant, learnerId: Long): Boolean =
    val dueDate: Option[AssessmentDueDate] = dueDateService.getDueDate(section, assessment, learnerId)
    dueDate match
      case Some(AssessmentDeadline(date)) => now.isAfter(date)
      case _                              => false
end AssessmentValidationUtils
