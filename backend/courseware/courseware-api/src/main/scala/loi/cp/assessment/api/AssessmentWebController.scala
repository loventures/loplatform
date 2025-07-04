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

import com.learningobjects.cpxp.component.annotation.{Controller, MatrixParam, PathVariable, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, Method}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.assessment.InstructorOverviews.{InstructorAttemptsOverview, UserGradingOverview}
import loi.cp.assessment.LearnerOverviews.LearnerAttemptOverview
import loi.cp.assessment.api.AssessmentWebController.UserGradingOverviewOverviewDto
import loi.cp.context.ContextId
import loi.cp.course.right.{ReadCourseRight, TeachCourseRight, ViewCourseGradeRight}
import loi.cp.reference.{ContentIdentifier, EdgePath}

import java.time.Instant
import scala.util.Try

/** A web controller for interacting with lightweight assessments. This provides readonly views to common
  * [[loi.cp.quiz.Quiz]] and [[loi.cp.submissionassessment.SubmissionAssessment]] data.
  */
@Controller(root = true, value = "assessment")
@RequestMapping(path = "assessment")
trait AssessmentWebController extends ApiRootComponent:

  /** Calculates an overview of all information necessary to grade user attempts for the specified assessment.
    *
    * @param contentIdentifier
    *   the id of the assessment
    * @return
    *   a grading overview for each learner with attempts against the assessment
    */
  @RequestMapping(path = "{identifier}/gradingOverview", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def getGradingOverview(
    @PathVariable("identifier") @SecuredAdvice contentIdentifier: ContentIdentifier
  ): Try[Seq[UserGradingOverviewOverviewDto]]

  /** Calculates an overview of the attempts the requested user has for the specified assessment.
    *
    * @param path
    *   the location of the assessment
    * @param context
    *   the context containing the assessment
    * @param userId
    *   the user to view as, otherwise self
    * @return
    *   an overview of the attempts the requested user has for the specified assessment
    */
  @RequestMapping(path = "{path}/attemptOverview", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[ReadCourseRight]))
  def getUserAttemptOverview(
    @PathVariable("path") path: EdgePath,
    @MatrixParam("context") @SecuredAdvice context: ContextId,
    @MatrixParam(required = false) userId: Option[JLong] = None
  ): Try[ArgoBody[LearnerAttemptOverview]]

  /** Calculates the overviews of the attempts the requested user has for the specified assessments.
    *
    * @param paths
    *   the location of the assessments
    * @param context
    *   the context containing the assessments
    * @param userId
    *   the user to view as, otherwise self
    * @return
    *   overviews of the attempts the requested user has for the specified assessments
    */
  @RequestMapping(path = "attemptOverview", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[ReadCourseRight]))
  def getUserAttemptOverviews(
    @MatrixParam("paths") paths: Seq[EdgePath],
    @MatrixParam("context") @SecuredAdvice context: ContextId,
    @MatrixParam(required = false) userId: Option[JLong] = None
  ): Try[ArgoBody[List[LearnerAttemptOverview]]]

  /** Calculates the attempt count overviews of the attempts for enrolled users in a course.
    *
    * @param paths
    *   the location of the assessments
    * @param context
    *   the context containing the assessments
    * @return
    *   overviews of users attempt counts by state
    */
  @RequestMapping(path = "instructorAttemptsOverview", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[ViewCourseGradeRight], classOf[CourseAdminRight]))
  def getInstructorAttemptsOverview(
    @MatrixParam("paths") paths: Seq[EdgePath],
    @MatrixParam("context") @SecuredAdvice context: ContextId
  ): ArgoBody[List[InstructorAttemptsOverview]]
end AssessmentWebController

object AssessmentWebController:

  case class UserProfileDto(
    id: Long,
    userName: String,
    givenName: String,
    middleName: String,
    familyName: String,
    emailAddress: String,
    externalId: Option[String],
  )

  case class UserGradingOverviewOverviewDto(
    identifier: ContentIdentifier,
    learner: UserProfileDto,
    mostRecentSubmission: Option[Instant],
    gradeableAttempts: Seq[AttemptId],
    attemptCount: Option[Int],
    invalidAttemptCount: Option[Int],
    hasViewableAttempts: Boolean,
    hasValidViewableAttempts: Boolean,
    grade: Option[Score]
  )

  object UserGradingOverviewOverviewDto:
    def apply(overview: UserGradingOverview): UserGradingOverviewOverviewDto =
      UserGradingOverviewOverviewDto(
        overview.identifier,
        UserProfileDto(
          overview.learner.id,
          overview.learner.userName,
          overview.learner.givenName,
          overview.learner.middleName,
          overview.learner.familyName,
          overview.learner.emailAddress,
          overview.learner.externalId,
        ),
        overview.mostRecentSubmission,
        overview.gradeableAttempts,
        Option(overview.attemptCount),
        Option(overview.invalidAttemptCount),
        overview.hasViewableAttempts,
        overview.hasValidViewableAttempts,
        overview.grade
      )
  end UserGradingOverviewOverviewDto
end AssessmentWebController
