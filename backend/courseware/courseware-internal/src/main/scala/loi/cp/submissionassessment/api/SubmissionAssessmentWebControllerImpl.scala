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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.api.AssessmentValidationUtils
import loi.cp.course.CourseAccessService
import loi.cp.reference.ContentIdentifier
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource

/** The default implementation of [[SubmissionAssessmentWebController]].
  */
@Component
class SubmissionAssessmentWebControllerImpl(
  val componentInstance: ComponentInstance,
  courseAccessService: CourseAccessService,
  submissionAssessmentWebUtils: SubmissionAssessmentWebUtils,
  assessmentValidationUtils: AssessmentValidationUtils,
  time: TimeSource,
  currentUser: => UserDTO
) extends SubmissionAssessmentWebController
    with ComponentImplementation:
  override def getSubmissionAssessment(
    id: ContentIdentifier,
    context: Long
  ): ErrorResponse \/ SubmissionAssessmentDto =

    for
      rights                <-
        courseAccessService.actualRights(id.contextId, currentUser) \/> ErrorResponse.forbidden
      (section, assessment) <- submissionAssessmentWebUtils
                                 .readSubmissionAssessment(id.contextId, id.edgePath, currentUser, rights.some)
                                 .leftMap(_.to404)
    yield SubmissionAssessmentDto(
      assessment,
      assessmentValidationUtils.isPastDeadline(section, assessment, time.instant, currentUser.id)
    )
end SubmissionAssessmentWebControllerImpl
