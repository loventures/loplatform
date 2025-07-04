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

package loi.cp.quiz

import loi.asset.assessment.model.AssessmentType.Formative
import loi.asset.assessment.model.ScoringOption.*
import loi.asset.assessment.model.*
import loi.authoring.asset.Asset
import loi.cp.assessment.AssessmentGradingPolicy
import loi.cp.assessment.AssessmentGradingPolicy.*
import loi.cp.assessment.settings.{AttemptLimit, Unlimited}
import loi.cp.quiz.settings.*

/** A trait wrapping an asset representing a [[Quiz]].
  */
sealed trait QuizAsset[T]:

  /** Returns the authored content for the quiz.
    *
    * @return
    *   the authored content for the quiz
    */
  def asset: Asset[T]

  /** Returns the title of the quiz.
    *
    * @return
    *   the title of the quiz
    */
  def title: String

  /** Returns the authored settings for the quiz.
    *
    * @return
    *   the authored settings for the quiz
    */
  def settings: QuizSettings

  /** Formative/summative type for the quiz.
    *
    * @return
    *   Formative or Summative, based on the quiz's underlying assessment type.
    */
  def assessmentType: AssessmentType

  /** Flag for whether the quiz should be treated as a diagnostic.
    *
    * @return
    *   true if the quiz is a diagnostic
    */
  def isDiagnostic: Boolean

  def isCheckpoint: Boolean

  def maxMinutes: Option[Long]

  /** The grading policy used to find the gradebook score for a quiz's attempts.
    *
    * @param scoringOption
    *   The underlying authored scoring option.
    * @return
    *   The grading policy used to create grades.
    */
  def gradingPolicy(scoringOption: Option[ScoringOption]): AssessmentGradingPolicy = scoringOption match
    case Some(FirstAttemptScore)         => FirstAttempt
    case Some(MostRecentAttemptScore)    => MostRecent
    case Some(HighestScore)              => Highest
    case Some(AverageScore)              => Average
    case Some(FullCreditOnAnyCompletion) => FullCreditOnCompletion
    case None                            => MostRecent
end QuizAsset

object QuizAsset:
  def apply(asset: Asset[?]): Option[QuizAsset[?]] = PartialFunction.condOpt(asset) {
    case Assessment.Asset(assessment)         => Assessment1QuizAsset(assessment)
    case Checkpoint.Asset(checkpoint)         => CheckpointQuizAsset(checkpoint)
    case Diagnostic.Asset(diagnostic)         => DiagnosticQuizAsset(diagnostic)
    case PoolAssessment.Asset(poolAssessment) => PoolAssessmentQuizAsset(poolAssessment)
  }

  def navigationPolicyOf(singlePage: Boolean, immediateFeedback: Boolean): NavigationPolicy =
    if singlePage then SinglePage
    else
      // There is currently no authored configuration that controls skipping
      Paged(skippingAllowed = true, backtrackingAllowed = !immediateFeedback)

  def resultsPolicyOf(immediateFeedback: Boolean, shouldHideAnswerIfIncorrect: Boolean): ResultsPolicy =
    val when: ResultReleaseTime =
      if immediateFeedback then ResultReleaseTime.OnResponseScore
      else ResultReleaseTime.OnAttemptScore

    val condition: ReleaseRemediationCondition =
      if shouldHideAnswerIfIncorrect then ReleaseRemediationCondition.OnCorrectResponse
      else ReleaseRemediationCondition.AnyResponse

    ResultsPolicy(when, condition)
  end resultsPolicyOf
end QuizAsset

case class Assessment1QuizAsset(asset: Asset[Assessment]) extends QuizAsset[Assessment]:
  override def title: String = asset.data.title

  override lazy val settings: QuizSettings =
    QuizSettings(
      attemptLimit,
      softAttemptLimit,
      softAttemptLimitMessage,
      navigationPolicy,
      resultsPolicy,
      gradingPolicy(asset.data.scoringOption),
      displayConfidenceIndicators,
      assessmentType,
      maxMinutes,
    )

  private val attemptLimit: AttemptLimit              = AttemptLimit.of(asset.data.maxAttempts.filter(_ != 0))
  private val softAttemptLimit: AttemptLimit          = AttemptLimit.of(asset.data.softAttemptLimit.filter(_ != 0))
  private val softAttemptLimitMessage: Option[String] = asset.data.softLimitMessage

  private val navigationPolicy: NavigationPolicy =
    QuizAsset.navigationPolicyOf(asset.data.singlePage, asset.data.immediateFeedback)

  private val resultsPolicy: ResultsPolicy =
    QuizAsset.resultsPolicyOf(asset.data.immediateFeedback, asset.data.shouldHideAnswerIfIncorrect)

  private val displayConfidenceIndicators: Boolean = asset.data.shouldDisplayConfidenceIndicator.contains(true)

  override def assessmentType: AssessmentType = asset.data.assessmentType

  override def isDiagnostic: Boolean = false

  override def isCheckpoint: Boolean = false

  override def maxMinutes: Option[Long] = asset.data.maxMinutes
end Assessment1QuizAsset

case class CheckpointQuizAsset(asset: Asset[Checkpoint]) extends QuizAsset[Checkpoint]:
  override def title: String = asset.data.title

  override lazy val settings: QuizSettings =
    QuizSettings(
      maxAttempts = Unlimited,
      softAttemptLimit = Unlimited,
      softAttemptLimitMessage = None,
      navigationPolicy = SinglePage,
      resultsPolicy,
      gradingPolicy = MostRecent,
      displayConfidenceIndicators = false,
      assessmentType,
      maxMinutes,
    )

  private val resultsPolicy: ResultsPolicy =
    QuizAsset.resultsPolicyOf(immediateFeedback = true, shouldHideAnswerIfIncorrect = false)

  override def assessmentType: AssessmentType = Formative

  override def isDiagnostic: Boolean = false

  override def isCheckpoint: Boolean = true

  override def maxMinutes: Option[Long] = None
end CheckpointQuizAsset

case class DiagnosticQuizAsset(asset: Asset[Diagnostic]) extends QuizAsset[Diagnostic]:
  override def title: String = asset.data.title

  override lazy val settings: QuizSettings =
    QuizSettings(
      attemptLimit,
      softAttemptLimit,
      softAttemptLimitMessage,
      navigationPolicy,
      resultsPolicy,
      gradingPolicy(asset.data.scoringOption),
      displayConfidenceIndicators,
      assessmentType,
      maxMinutes,
    )

  private val attemptLimit: AttemptLimit              = AttemptLimit.of(asset.data.maxAttempts.filter(_ != 0))
  private val softAttemptLimit: AttemptLimit          = Unlimited
  private val softAttemptLimitMessage: Option[String] = None

  private val navigationPolicy: NavigationPolicy =
    QuizAsset.navigationPolicyOf(asset.data.singlePage, asset.data.immediateFeedback)

  private val resultsPolicy: ResultsPolicy =
    QuizAsset.resultsPolicyOf(asset.data.immediateFeedback, asset.data.shouldHideAnswerIfIncorrect)

  private val displayConfidenceIndicators: Boolean = asset.data.shouldDisplayConfidenceIndicator.contains(true)

  override def assessmentType: AssessmentType = asset.data.assessmentType

  override def isDiagnostic: Boolean = true

  override def isCheckpoint: Boolean = false

  override def maxMinutes: Option[Long] = asset.data.maxMinutes
end DiagnosticQuizAsset

case class PoolAssessmentQuizAsset(asset: Asset[PoolAssessment]) extends QuizAsset[PoolAssessment]:
  override def title: String = asset.data.title

  override lazy val settings: QuizSettings =
    QuizSettings(
      attemptLimit,
      softAttemptLimit,
      softAttemptLimitMessage,
      navigationPolicy,
      resultsPolicy,
      gradingPolicy(asset.data.scoringOption),
      displayConfidenceIndicators,
      assessmentType,
      maxMinutes,
    )

  private val attemptLimit: AttemptLimit              = AttemptLimit.of(asset.data.maxAttempts.filter(_ != 0))
  private val softAttemptLimit: AttemptLimit          = Unlimited
  private val softAttemptLimitMessage: Option[String] = None

  private val navigationPolicy: NavigationPolicy =
    QuizAsset.navigationPolicyOf(asset.data.singlePage, asset.data.immediateFeedback)

  private val resultsPolicy: ResultsPolicy =
    QuizAsset.resultsPolicyOf(asset.data.immediateFeedback, asset.data.shouldHideAnswerIfIncorrect)

  private val displayConfidenceIndicators: Boolean = asset.data.shouldDisplayConfidenceIndicator.contains(true)

  override def assessmentType: AssessmentType = asset.data.assessmentType

  override def isDiagnostic: Boolean = false

  override def isCheckpoint: Boolean = false

  override def maxMinutes: Option[Long] = asset.data.maxMinutes
end PoolAssessmentQuizAsset
