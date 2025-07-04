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

import loi.asset.assessment.model.*
import loi.asset.contentpart.BlockPart
import loi.authoring.render.LtiLinkRenderer
import loi.cp.assessment.settings.AttemptLimit
import loi.cp.assessment.{AssessmentGradingPolicy, CourseAssessmentPolicy}
import loi.cp.content.{CourseContent, courseAssetInstructionsUrl}
import loi.cp.course.CourseSection
import loi.cp.instructions.{BlockInstructions, Instructions}
import loi.cp.quiz.settings.QuizSettings
import loi.cp.reference.{ContentIdentifier, EdgePath, VersionedAssetReference}
import scalaz.std.option.*
import scalaz.std.tuple.*
import scalaz.syntax.bitraverse.*

/** A service object for a quiz. Quizzes are assessments that ask the user a series of questions. Responses may be
  * automatically or manually graded. Questions may or may not be deterministic per attempt depending on the type of
  * quiz.
  *
  * Quizzes may or may not have contextual configurations.
  */
case class Quiz(
  content: QuizAsset[?],
  courseContent: CourseContent,
  instructions: Option[Instructions],
  assetReference: VersionedAssetReference,
  contentId: ContentIdentifier,
  section: CourseSection,
  coursePolicyAssessmentSetting: Option[CourseAssessmentPolicy],
  testsOut: Map[EdgePath, Double]
) extends loi.cp.assessment.Assessment:

  /** Returns the title of the quiz.
    *
    * @return
    *   the title of the quiz
    */
  val title: String = content.title

  /** Returns the settings for the quiz. This includes any additional {{QuizConfiguration}} particular to this activity
    * in a context.
    *
    * @return
    *   the settings for the quiz
    */
  val settings: QuizSettings = coursePolicyAssessmentSetting.map(content.settings.customize).getOrElse(content.settings)

  /** Returns whether the assessment is formative or summative.
    *
    * @return
    *   whether the assessment is formative or summative
    */
  val assessmentType: AssessmentType = content.assessmentType

  val isDiagnostic: Boolean = content.isDiagnostic

  val isCheckpoint: Boolean = content.isCheckpoint

  override val maxAttempts: AttemptLimit = settings.maxAttempts

  override val gradingPolicy: AssessmentGradingPolicy = settings.gradingPolicy
end Quiz

object Quiz:
  def fromContent(
    content: CourseContent,
    section: CourseSection,
    settings: List[CourseAssessmentPolicy]
  ): Option[Quiz] =

    QuizAsset(content.asset).map(quizAsset =>

      val rewriteLti = LtiLinkRenderer.rewriteContentPart[BlockPart](_, courseAssetInstructionsUrl(section, content))

      val instructions = content.asset.data match
        case assessment: Assessment         => Option(assessment.instructions).map(rewriteLti).map(BlockInstructions.apply)
        case checkpoint: Checkpoint         => Option(checkpoint.instructions).map(rewriteLti).map(BlockInstructions.apply)
        case diagnostic: Diagnostic         => Option(diagnostic.instructions).map(rewriteLti).map(BlockInstructions.apply)
        case poolAssessment: PoolAssessment =>
          Option(poolAssessment.instructions).map(rewriteLti).map(BlockInstructions.apply)
        case _                              => None

      val coursePolicySetting = settings.find(_.assessmentType.doesAssessmentMatch(quizAsset.asset))

      Quiz(
        content = quizAsset,
        courseContent = content,
        instructions = instructions,
        assetReference = VersionedAssetReference(quizAsset.asset, section.commitId),
        contentId = ContentIdentifier(section, content.edgePath),
        section = section,
        coursePolicySetting,
        testsOut = content.testsOut.flatMap(_.bitraverse(section.contents.get(_).map(_.edgePath), Some.apply))
      )
    )
end Quiz
