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

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.assessment.Assessment
import loi.cp.course.CourseSection

/** A service to compute content due dates and apply the contextual meanings of how the due date applies to the
  * [[loi.cp.assessment.Assessment]].
  */
@Service
trait AssessmentDueDateService:

  /** Returns the due date, if any, for the given {{assessment}}, based on its content.
    *
    * @param section
    *   the course containing the assessments
    * @param assessment
    *   the assessment to calculate the due date for
    * @param learnerId
    *   the learner to calculate the due date for
    * @return
    *   the due date for the given assessment
    */
  def getDueDate(section: CourseSection, assessment: Assessment, learnerId: Long) =
    getDueDates(section, Seq(assessment), learnerId)(assessment)

  /** Returns the due date, if any, for the given {{assessments}}, based on their content.
    *
    * @param section
    *   the course containing the assessments
    * @param assessments
    *   the assessments to calculate dues date for
    * @param learnerId
    *   the learner to calculate the due date for
    * @return
    *   the due dates for the given assessments
    */
  def getDueDates(
    section: CourseSection,
    assessments: Seq[Assessment],
    learnerId: Long
  ): Map[Assessment, Option[AssessmentDueDate]]
end AssessmentDueDateService
