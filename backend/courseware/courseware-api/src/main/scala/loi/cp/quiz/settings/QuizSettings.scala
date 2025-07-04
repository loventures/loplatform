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

package loi.cp.quiz.settings

import loi.asset.assessment.model.AssessmentType
import loi.cp.assessment.settings.AttemptLimit
import loi.cp.assessment.{AssessmentGradingPolicy, CourseAssessmentPolicy}

/** The particular settings for a quiz. This may or may not have a configurations overlay applied to it.
  */
case class QuizSettings(
  maxAttempts: AttemptLimit,
  softAttemptLimit: AttemptLimit,
  softAttemptLimitMessage: Option[String],
  navigationPolicy: NavigationPolicy,
  resultsPolicy: ResultsPolicy,
  gradingPolicy: AssessmentGradingPolicy,
  displayConfidenceIndicators: Boolean,
  assessmentType: AssessmentType,
  maxMinutes: Option[Long],
):
  def customize(coursePolicyAssessmentSetting: CourseAssessmentPolicy): QuizSettings =
    this.copy(
      maxAttempts = coursePolicyAssessmentSetting.attemptLimit,
      gradingPolicy = coursePolicyAssessmentSetting.assessmentGradingPolicy
    )
end QuizSettings
