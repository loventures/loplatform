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

package loi.cp.submissionassessment

import loi.asset.assessment.model.ScoringOption.*
import loi.asset.assessment.model.*
import loi.authoring.asset.Asset
import loi.cp.assessment.AssessmentGradingPolicy
import loi.cp.assessment.AssessmentGradingPolicy.*
import loi.cp.assessment.settings.{AttemptLimit, Limited, Unlimited}
import loi.cp.submissionassessment.settings.AuthoredSubmissionAssessmentSettings
import loi.cp.submissionassessment.settings.SubmissionAssessmentDriver.{Observation, SubjectDriven}

/** A trait for an asset representing a [[SubmissionAssessment]].
  */
sealed trait SubmissionAssessmentAsset[T]:

  /** Returns the authored content for the assessment.
    *
    * @return
    *   the authored content for the assessment
    */
  def asset: Asset[T]

  /** Returns the authored settings for the assessment.
    *
    * @return
    *   the authored settings for the assessment
    */
  def settings: AuthoredSubmissionAssessmentSettings

  /** Formative/summative type for the assessment.
    *
    * @return
    *   Formative or Summative, based on the assessment's underlying assessment type.
    */
  def assessmentType: AssessmentType

  /** Flag for whether the assessment should be treated as a diagnostic.
    *
    * @return
    *   true if the assessment is a diagnostic
    */
  def isDiagnostic: Boolean
end SubmissionAssessmentAsset

object SubmissionAssessmentAsset:
  def apply(asset: Asset[?]): Option[SubmissionAssessmentAsset[?]] =
    asset match
      case Assignment1.Asset(assignment1)                       => Some(Assignment1SubmissionAssessmentAsset(assignment1))
      case ObservationAssessment1.Asset(observationAssignment1) =>
        Some(ObservationAssessment1SubmissionAssessmentAsset(observationAssignment1))
      case _                                                    => None

  /** The grading policy used to find the gradebook score for an assessment's attempts.
    *
    * @param scoringOption
    *   the underlying authored scoring option.
    * @return
    *   The grading policy used to create grades.
    */
  def gradingPolicy(scoringOption: Option[ScoringOption]): AssessmentGradingPolicy =
    scoringOption match
      case Some(FirstAttemptScore)         => FirstAttempt
      case Some(MostRecentAttemptScore)    => MostRecent
      case Some(HighestScore)              => Highest
      case Some(AverageScore)              => Average
      case Some(FullCreditOnAnyCompletion) => FullCreditOnCompletion
      case None                            => MostRecent
end SubmissionAssessmentAsset

case class Assignment1SubmissionAssessmentAsset(asset: Asset[Assignment1])
    extends SubmissionAssessmentAsset[Assignment1]:
  override def settings: AuthoredSubmissionAssessmentSettings =
    val attemptLimit: AttemptLimit =
      if asset.data.unlimitedAttempts || asset.data.maxAttempts.isEmpty then Unlimited
      else Limited(asset.data.maxAttempts.get.toInt)

    AuthoredSubmissionAssessmentSettings(
      SubjectDriven,
      allowEssays = true,
      attemptLimit,
      SubmissionAssessmentAsset.gradingPolicy(asset.data.scoringOption)
    )
  end settings

  override def assessmentType: AssessmentType = asset.data.assessmentType

  override def isDiagnostic: Boolean = false
end Assignment1SubmissionAssessmentAsset

case class ObservationAssessment1SubmissionAssessmentAsset(asset: Asset[ObservationAssessment1])
    extends SubmissionAssessmentAsset[ObservationAssessment1]:
  override def settings: AuthoredSubmissionAssessmentSettings =
    val attemptLimit: AttemptLimit =
      if asset.data.unlimitedAttempts || asset.data.maxAttempts.isEmpty then Unlimited
      else Limited(asset.data.maxAttempts.get.toInt)

    AuthoredSubmissionAssessmentSettings(
      Observation,
      allowEssays = false,
      attemptLimit,
      SubmissionAssessmentAsset.gradingPolicy(asset.data.scoringOption)
    )
  end settings

  override def assessmentType: AssessmentType = asset.data.assessmentType

  override def isDiagnostic: Boolean = false
end ObservationAssessment1SubmissionAssessmentAsset
