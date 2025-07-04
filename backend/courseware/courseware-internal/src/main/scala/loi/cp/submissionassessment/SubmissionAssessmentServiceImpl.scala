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

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.assessment.CourseAssessmentPolicy
import loi.cp.assessment.rubric.RubricService
import loi.cp.competency.CompetentCompetencyService
import loi.cp.content.CourseContent
import loi.cp.course.CourseSection
import loi.cp.reference.*

/** The worst implementation of [[SubmissionAssessmentService]].
  */
@Service
class SubmissionAssessmentServiceImpl(
  rubricService: RubricService,
  submissionInstructionService: SubmissionInstructionService,
  competentCompetencyService: CompetentCompetencyService,
) extends SubmissionAssessmentService:
  override def getSubmissionAssessments(
    course: CourseSection,
    contents: Seq[CourseContent],
    policies: List[CourseAssessmentPolicy],
    ws: AttachedReadWorkspace,
  ): Seq[SubmissionAssessment] =

    val submissionAssets = contents.flatMap(content =>
      SubmissionAssessmentAsset(content.asset).map(assessmentAsset => content -> assessmentAsset)
    )

    val instructionsForContent = submissionInstructionService.getInstructions(course.lwc, contents)
    val contentToCompetencies  =
      competentCompetencyService.getDirectlyAssessedCompetencies(ws, contents.map(_.asset))
    val allRubrics             = rubricService.getRubrics(ws, submissionAssets.map(_._2.asset))
    for (content, assessmentAsset) <- submissionAssets
    yield
      val contentId              = ContentIdentifier(course, content.edgePath)
      val reference              = VersionedAssetReference(content.asset, course.commitId)
      val rubric                 = allRubrics.get(content.asset)
      val instructions           = instructionsForContent(content)
      val assessmentCompetencies =
        contentToCompetencies.getOrElse(content.name, Nil)

      SubmissionAssessmentImpl(
        assessmentAsset,
        content,
        rubric,
        assessmentCompetencies,
        instructions,
        reference,
        contentId,
        course,
        policies.find(_.assessmentType.doesAssessmentMatch(assessmentAsset.asset))
      )
    end for
  end getSubmissionAssessments
end SubmissionAssessmentServiceImpl
