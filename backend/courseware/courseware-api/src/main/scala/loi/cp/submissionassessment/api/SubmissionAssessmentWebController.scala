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

package loi.cp.submissionassessment.api

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method}
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.assessment.rubric.AssessmentRubric
import loi.cp.course.right.{ReadCourseRight, TeachCourseRight}
import loi.cp.instructions.Instructions
import loi.cp.reference.{ContentIdentifier, VersionedAssetReference}
import loi.cp.submissionassessment.SubmissionAssessment
import loi.cp.submissionassessment.settings.AuthoredSubmissionAssessmentSettings
import scalaz.\/

/** A web controller for serving [[loi.cp.submissionassessment.SubmissionAssessment]] s
  */
@Controller(root = true, value = "submissionAssessment")
@RequestMapping(path = "submissionAssessment")
trait SubmissionAssessmentWebController extends ApiRootComponent:
  @RequestMapping(path = "{id}", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[ReadCourseRight]))
  def getSubmissionAssessment(
    @PathVariable("id") id: ContentIdentifier,
    @SecuredAdvice @MatrixParam("context") context: Long
  ): ErrorResponse \/ SubmissionAssessmentDto

case class SubmissionAssessmentDto(
  assetReference: VersionedAssetReference,
  contentId: ContentIdentifier,
  title: String,
  settings: AuthoredSubmissionAssessmentSettings,
  instructions: Option[Instructions],
  rubric: Option[AssessmentRubric],
  pastDeadline: Boolean
)

object SubmissionAssessmentDto:
  def apply(
    submissionAssessment: SubmissionAssessment,
    pastDeadline: Boolean
  ): SubmissionAssessmentDto =
    SubmissionAssessmentDto(
      submissionAssessment.assetReference,
      submissionAssessment.contentId,
      submissionAssessment.title,
      submissionAssessment.settings,
      submissionAssessment.instructions,
      submissionAssessment.rubric,
      pastDeadline
    )
end SubmissionAssessmentDto
