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

package loi.cp.grade

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.ApiPage
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.{ArgoBody, ErrorResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.attempt.AssessmentAttemptService
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.course.CourseWorkspaceService
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.quiz.Quiz
import loi.cp.submissionassessment.SubmissionAssessmentService
import loi.cp.submissionassessment.settings.SubmissionAssessmentDriver.Observation
import scalaz.\/
import scaloi.syntax.collection.*

/** The default implementation of [[GradingQueueWebController]]
  */
@Component
class GradingQueueWebControllerImpl(val componentInstance: ComponentInstance)(
  courseWebUtils: CourseWebUtils,
  assessmentAttemptService: AssessmentAttemptService,
  submissionAssessmentService: SubmissionAssessmentService,
  userDTO: => UserDTO,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  courseWorkspaceService: CourseWorkspaceService
) extends GradingQueueWebController
    with ComponentImplementation:

  /** Returns keys and statistics of content that needs instructor attention.
    *
    * @param contextId
    *   the context id
    * @param page
    *   any paging that should be applied to the results
    * @return
    *   the items that need instructor attention
    */
  override def getGradingQueue(
    contextId: ContextId,
    page: ApiPage,
  ): ErrorResponse \/ ArgoBody[List[GradingQueueDto]] =
    for section <- courseWebUtils.loadCourseSection(contextId.id).leftMap(_.to404)
    yield
      val ws                           = courseWorkspaceService.loadReadWorkspace(section)
      val policies                     = courseAssessmentPoliciesService.getPolicies(section)
      val allSubmissionAssessments     =
        submissionAssessmentService.getSubmissionAssessments(section, section.contents.nonRootElements, List.empty, ws)
      val studentSubmissionAssessments = allSubmissionAssessments.filter(_.settings.driver != Observation)
      val quizzes                      = section.contents.nonRootElements.flatMap(c => Quiz.fromContent(c, section, policies))

      val overviews =
        assessmentAttemptService
          .getParticipationData(section, quizzes ++ studentSubmissionAssessments)
          .groupMapUniq(_.identifier.edgePath)(ParticipationOverview.apply)

      val gradingQueue: Seq[GradingQueueDto] =
        for
          content         <- section.contents.nonRootElements
          contentOverview <- overviews.get(content.edgePath)
          if contentOverview.actionItemCount > 0
        yield GradingQueueDto(contextId, content.edgePath, contentOverview)

      ArgoBody(gradingQueue.toList)
end GradingQueueWebControllerImpl
