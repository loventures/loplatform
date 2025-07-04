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
import loi.asset.assessment.model.{Assignment1, ObservationAssessment1}
import loi.authoring.render.LtiLinkRenderer
import loi.cp.content.{CourseContent, courseAssetInstructionsUrl}
import loi.cp.course.lightweight.Lwc
import loi.cp.instructions.{BlockInstructions, Instructions}

/** A service that can derive instructions for a submission assessment asset
  */
@Service
trait SubmissionInstructionService:
  def getInstructions(
    course: Lwc,
    contents: Seq[CourseContent]
  ): Map[CourseContent, Option[Instructions]]

@Service
class SubmissionInstructionServiceImpl extends SubmissionInstructionService:

  def getInstructions(
    course: Lwc,
    contents: Seq[CourseContent]
  ): Map[CourseContent, Option[Instructions]] =

    contents
      .flatMap(content =>
        PartialFunction.condOpt(content.asset) {
          case Assignment1.Asset(assignment1) =>
            content ->
              Option(assignment1.data.instructions)
                .map(LtiLinkRenderer.rewriteContentPart(_, courseAssetInstructionsUrl(course, content)))
                .map(BlockInstructions.apply)

          case ObservationAssessment1.Asset(observation1) =>
            content ->
              Option(observation1.data.instructions)
                .map(LtiLinkRenderer.rewriteContentPart(_, courseAssetInstructionsUrl(course, content)))
                .map(BlockInstructions.apply)
        }
      )
      .toMap
  end getInstructions
end SubmissionInstructionServiceImpl
