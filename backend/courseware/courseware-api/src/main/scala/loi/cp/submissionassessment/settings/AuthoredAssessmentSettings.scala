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

package loi.cp.submissionassessment.settings

import enumeratum.{Enum, EnumEntry}
import loi.cp.assessment.{AssessmentGradingPolicy, CourseAssessmentPolicy}
import loi.cp.assessment.settings.AttemptLimit

/** The authored settings for any [[loi.cp.submissionassessment.SubmissionAssessment]].
  */
case class AuthoredSubmissionAssessmentSettings(
  driver: Driver,
  allowEssays: Boolean,
  maxAttempts: AttemptLimit,
  gradingPolicy: AssessmentGradingPolicy
):
  def customize(policy: CourseAssessmentPolicy): AuthoredSubmissionAssessmentSettings =
    this.copy(
      maxAttempts = policy.attemptLimit,
      gradingPolicy = policy.assessmentGradingPolicy
    )
end AuthoredSubmissionAssessmentSettings

sealed trait Driver extends EnumEntry

case object SubmissionAssessmentDriver extends Enum[Driver]:
  val values = findValues

  /** the subject of assessment can start, response to and submit attempts */
  case object SubjectDriven extends Driver

  /** the instructor for the assessment can start, response to and submit attempts */
  case object Observation extends Driver
