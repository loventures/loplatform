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

import loi.asset.assessment.model.AssessmentType
import loi.cp.assessment.rubric.AssessmentRubric
import loi.cp.assessment.settings.AttemptLimit
import loi.cp.assessment.{AssessmentGradingPolicy, CourseAssessmentPolicy}
import loi.cp.competency.Competency
import loi.cp.content.CourseContent
import loi.cp.course.CourseSection
import loi.cp.instructions.Instructions
import loi.cp.reference.{ContentIdentifier, VersionedAssetReference}
import loi.cp.submissionassessment.settings.AuthoredSubmissionAssessmentSettings

/** The default implementation of {{SubmissionAssessment}}.
  *
  * @param content
  *   the authored content
  * @param courseContent
  *   the course content
  * @param rubric
  *   the rubric
  * @param assessmentCompetencies
  *   the competencies directly associated with the assessment
  * @param instructions
  *   the instructions for the assessment, if any
  * @param reference
  *   the reference to authored content {{content}}
  * @param contentId
  *   the contextual reference for the quiz
  */
case class SubmissionAssessmentImpl(
  content: SubmissionAssessmentAsset[?],
  courseContent: CourseContent,
  rubric: Option[AssessmentRubric],
  assessmentCompetencies: Seq[Competency],
  instructions: Option[Instructions],
  reference: VersionedAssetReference,
  contentId: ContentIdentifier,
  section: CourseSection,
  policy: Option[CourseAssessmentPolicy]
) extends SubmissionAssessment:
  override def title: String = courseContent.title

  override def assetReference: VersionedAssetReference = reference

  override def assessmentType: AssessmentType = content.assessmentType

  override def settings: AuthoredSubmissionAssessmentSettings =
    policy.map(content.settings.customize).getOrElse(content.settings)

  override val maxAttempts: AttemptLimit              = settings.maxAttempts
  override val gradingPolicy: AssessmentGradingPolicy = settings.gradingPolicy
end SubmissionAssessmentImpl
