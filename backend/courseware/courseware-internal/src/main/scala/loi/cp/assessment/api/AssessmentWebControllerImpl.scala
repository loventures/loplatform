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

package loi.cp.assessment
package api

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ArgoBody
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.assessment.InstructorOverviews.InstructorAttemptsOverview
import loi.cp.assessment.LearnerOverviews.LearnerAttemptOverview
import loi.cp.assessment.api.AssessmentWebController.UserGradingOverviewOverviewDto
import loi.cp.assessment.attempt.AssessmentAttemptService
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.course.CourseEnrollmentService
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.reference.{ContentIdentifier, EdgePath}
import loi.cp.user.ImpersonationService
import scalaz.syntax.std.list.*
import scalaz.syntax.std.option.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.option.*

import scala.util.Try

@Component
class AssessmentWebControllerImpl(val componentInstance: ComponentInstance, currentUser: => UserDTO)(implicit
  assessmentService: AssessmentService,
  assessmentAttemptService: AssessmentAttemptService,
  courseEnrollmentService: CourseEnrollmentService,
  courseWebUtils: CourseWebUtils,
  impersonationService: ImpersonationService,
  cs: ComponentService,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  readWorkspaceService: ReadWorkspaceService
) extends AssessmentWebController
    with ComponentImplementation:
  override def getGradingOverview(contentIdentifier: ContentIdentifier): Try[Seq[UserGradingOverviewOverviewDto]] =

    val (course, ws) = courseWebUtils.sectionWsOrThrow404(contentIdentifier.contextId.id)

    for content <- course.contents.get_!(contentIdentifier.edgePath)
    yield
      val policies             = courseAssessmentPoliciesService.getPolicies(course)
      val assessments          = assessmentService.getAssessments(course, Seq(content), policies, ws)
      // This is the only caller of AssessmentAttemptService#getAttempts which is a bad and misguided method
      // considering a long-running course with .. a bunch of attempts
      val attemptsByAssessment = assessmentAttemptService.getAttempts(course, assessments)

      val usersNel = courseEnrollmentService
        .getEnrolledStudentDTOs(contentIdentifier.contextId.value)
        .toNel

      val overviewInfo = usersNel
        .map(users =>
          assessmentAttemptService
            .getGradingOverviews(contentIdentifier.contextId, course.contents, attemptsByAssessment, users)
        )
        .getOrElse(Nil)

      overviewInfo.map(UserGradingOverviewOverviewDto(_))
    end for
  end getGradingOverview

  override def getUserAttemptOverview(
    path: EdgePath,
    context: ContextId,
    viewAsId: Option[JLong]
  ): Try[ArgoBody[LearnerAttemptOverview]] =
    val viewAs: UserId = viewAsId.cata(UserId(_), currentUser)

    val (course, ws) = courseWebUtils.sectionWsOrThrow404(context.id)

    for
      _       <- impersonationService.checkImpersonation(context, viewAs).toTry
      content <- course.contents.get(path).toTry(new ResourceNotFoundException(s"No such content $path"))
    yield
      val policies    = courseAssessmentPoliciesService.getPolicies(course)
      val assessments = assessmentService.getAssessments(course, Seq(content), policies, ws)
      val overview    = assessmentAttemptService.getLearnerAttemptOverviews(course, assessments, viewAs)
      ArgoBody(overview.head)
  end getUserAttemptOverview

  override def getUserAttemptOverviews(
    paths: Seq[EdgePath],
    context: ContextId,
    viewAsId: Option[JLong]
  ): Try[ArgoBody[List[LearnerAttemptOverview]]] =
    val viewAs: UserId = viewAsId.cata(UserId(_), currentUser)
    val (course, ws)   = courseWebUtils.sectionWsOrThrow404(context.id)

    for
      _                <- impersonationService.checkImpersonation(context, viewAs).toTry
      requestedContents = course.contents.nonRootElements.filter(content => paths.contains(content.edgePath))
    yield
      val policies    = courseAssessmentPoliciesService.getPolicies(course)
      val assessments = assessmentService.getAssessments(course, requestedContents, policies, ws)
      val overviews   = assessmentAttemptService.getLearnerAttemptOverviews(course, assessments, viewAs)
      ArgoBody(overviews.toList)
  end getUserAttemptOverviews

  override def getInstructorAttemptsOverview(
    paths: Seq[EdgePath],
    context: ContextId
  ): ArgoBody[List[InstructorAttemptsOverview]] =
    val (course, ws)      = courseWebUtils.sectionWsOrThrow404(context.id)
    val requestedContents = course.contents.nonRootElements.filter(content => paths.contains(content.edgePath))
    val policies          = courseAssessmentPoliciesService.getPolicies(course)
    val assessments       = assessmentService.getAssessments(course, requestedContents, policies, ws)
    val overviews         = assessmentAttemptService.getInstructorAttemptsOverviews(course, assessments)
    ArgoBody(overviews.toList)
  end getInstructorAttemptsOverview
end AssessmentWebControllerImpl
