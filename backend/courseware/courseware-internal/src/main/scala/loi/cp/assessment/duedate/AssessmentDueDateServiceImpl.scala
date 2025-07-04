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
import loi.cp.assessment.duedate.AssessmentDueDateType.{DeadlineType, TargetDateType}
import loi.cp.course.{CourseConfigurationService, CoursePreferences, CourseSection}
import loi.cp.duedate.StoragedDueDateExemptions
import loi.cp.storage.CourseStorageService

import java.time.Instant

/** A service that binds the business logic for where to find the due date configuration and the course content due
  * date.
  */
@Service
class AssessmentDueDateServiceImpl(
  courseConfigurationService: CourseConfigurationService,
  storageService: CourseStorageService
) extends AssessmentDueDateService:
  override def getDueDates(
    section: CourseSection,
    assessments: Seq[Assessment],
    learnerId: Long
  ): Map[Assessment, Option[AssessmentDueDate]] =
    val dueDateType: AssessmentDueDateType = getConfiguration(section)

    assessments
      .map(assessment =>
        val dueDate: Option[AssessmentDueDate] =
          section.courseDueDate(assessment.edgePath).map(build(_, dueDateType, section, learnerId))
        assessment -> dueDate
      )
      .toMap
  end getDueDates

  private def build(
    date: Instant,
    typ: AssessmentDueDateType,
    section: CourseSection,
    learnerId: Long
  ): AssessmentDueDate =
    val exemptions: StoragedDueDateExemptions = storageService.get[StoragedDueDateExemptions](section)

    typ match
      case TargetDateType => AssessmentTargetDate(date)
      case DeadlineType   =>
        if exemptions.value.contains(learnerId) then AssessmentDeadlineExempted(date)
        else AssessmentDeadline(date)
  end build

  private def getConfiguration(section: CourseSection): AssessmentDueDateType =
    val preferences = courseConfigurationService.getGroupConfig(CoursePreferences, section)
    if preferences.strictDueDate then DeadlineType
    else TargetDateType
end AssessmentDueDateServiceImpl
