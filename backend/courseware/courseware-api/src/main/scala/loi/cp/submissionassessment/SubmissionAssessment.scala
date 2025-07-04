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
import loi.cp.assessment.Assessment
import loi.cp.assessment.rubric.AssessmentRubric
import loi.cp.competency.Competency
import loi.cp.instructions.Instructions
import loi.cp.reference.{ContentIdentifier, VersionedAssetReference}
import loi.cp.submissionassessment.settings.AuthoredSubmissionAssessmentSettings

/** An assessment whose attempt are uploaded submissions. Attempts may be driven by either a user or an instructor
  * depending upon configurations.
  */
trait SubmissionAssessment extends Assessment:

  /** Returns the title of the assessment.
    *
    * @return
    *   the title of the assessment
    */
  def title: String

  /** Returns the reference to the particular content used for this assessment. This does not cover the particular usage
    * of the assessment in the course.
    *
    * @return
    *   the reference to the particular content used for this assessment
    */
  def assetReference: VersionedAssetReference

  /** Returns where the content is being used in a context.
    *
    * @return
    *   where the content is being used in a context
    */
  def contentId: ContentIdentifier

  /** Returns the instructions for the assessment, if any.
    *
    * @return
    *   the instructions for the assessment, if any
    */
  def instructions: Option[Instructions]

  /** Returns the rubric, if any, associated with the assessment.
    *
    * @return
    *   the rubric associated with the assessment; otherwise [[None]]
    */
  def rubric: Option[AssessmentRubric]

  /** Returns <i>all</i> competencies associated with this assessment, both from the rubric and directly from the
    * assessment.
    *
    * @return
    */
  def competencies: Seq[Competency] = (rubric.map(_.competencies).getOrElse(Nil) ++ assessmentCompetencies).distinct

  /** Returns the competencies directly attached to this assessment. This does not include competencies from the rubric.
    *
    * @return
    */
  def assessmentCompetencies: Seq[Competency]

  /** Returns whether the assessment is formative or summative.
    *
    * @return
    *   whether the assessment is formative or summative
    */
  def assessmentType: AssessmentType

  /** Returns the settings for the assessment.
    *
    * @return
    *   the settings for the assessment
    */
  def settings: AuthoredSubmissionAssessmentSettings
end SubmissionAssessment
