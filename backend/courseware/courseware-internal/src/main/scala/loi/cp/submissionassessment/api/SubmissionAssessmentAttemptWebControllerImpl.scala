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
package api

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.{ArgoBody, ErrorResponse, FileResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import com.learningobjects.cpxp.util.FileInfo
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.assessment.api.{AssessmentValidationUtils, AttemptValidationUtils, ResponseFeedbackUtils}
import loi.cp.assessment.attempt.AttemptState.Finalized
import loi.cp.assessment.{AttemptId, CourseAssessmentPolicy, Feedback}
import loi.cp.attachment.{AttachmentId, AttachmentInfo, InvalidUploads}
import loi.cp.config.ConfigurationService
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.course.{
  CourseAccessService,
  CourseConfigurationService,
  CoursePreferences,
  CourseSection,
  CourseWorkspaceService
}
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.reference.{ContentIdentifier, EdgePath}
import loi.cp.security.SecuritySettings
import loi.cp.submissionassessment.attachment.SubmissionAssessmentAttachmentService
import loi.cp.submissionassessment.attempt.{
  AttemptNotFound,
  SubmissionAttempt,
  SubmissionAttemptFailure,
  SubmissionAttemptService
}
import loi.cp.user.ImpersonationService
import loi.cp.user.web.UserWebUtils
import scalaz.std.list.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scalaz.{ValidationNel, \/}
import scaloi.misc.TimeSource
import scaloi.syntax.option.*

@Component
class SubmissionAssessmentAttemptWebControllerImpl(val componentInstance: ComponentInstance)(
  courseWebUtils: CourseWebUtils,
  assessmentValidationUtils: AssessmentValidationUtils,
  courseAccessService: CourseAccessService,
  courseConfigurationService: CourseConfigurationService,
  submissionAssessmentWebUtils: SubmissionAssessmentWebUtils,
  currentUser: => UserDTO,
  impersonationService: ImpersonationService,
  submissionAssessmentAttachmentService: SubmissionAssessmentAttachmentService,
  submissionAttemptService: SubmissionAttemptService,
  now: TimeSource,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  userWebUtils: UserWebUtils,
  courseWorkspaceService: CourseWorkspaceService,
)(implicit configurationService: ConfigurationService)
    extends ComponentImplementation
    with SubmissionAssessmentAttemptWebController:

  import AttemptValidationUtils.*
  import ResponseFeedbackUtils.*

  private def alwaysReleaseFeedbackForContext(contextId: ContextId) =
    val coursePrefs = courseConfigurationService.getGroupConfig(CoursePreferences, contextId)
    coursePrefs.alwaysReleaseFeedback

  /** Loads a submission attempt, given it is readable (both the attempt and assessment) by a given user.
    *
    * @param loadAttempt
    *   a method to load the attempt appropriately
    * @param attemptId
    *   the id of the attempt
    * @param contextId
    *   the context from the request
    * @param asUser
    *   the user to view as
    * @return
    *   the section and attempt, if the user can read the assessment and attempt and is able to view as the given user
    */
  private def validatedSectionAndAttempt(
    loadAttempt: (CourseSection, AttachedReadWorkspace, Long, List[CourseAssessmentPolicy]) => Option[SubmissionAttempt]
  )(
    attemptId: Long,
    contextId: Long,
    asUser: UserId
  ): ErrorResponse \/ (CourseSection, SubmissionAttempt) =
    for
      section <- courseWebUtils.loadCourseSection(contextId).leftMap(_.to404)
      _       <- impersonationService.checkImpersonation(section.lwc, asUser).leftMap(_.getMessage.to404)
      policies = courseAssessmentPoliciesService.getPolicies(section)
      ws       = courseWorkspaceService.loadReadWorkspace(section)
      attempt <- loadAttempt(section, ws, attemptId, policies)
                   .toRightDisjunction(AttemptNotFound(attemptId).message.to404)
      _       <- submissionAssessmentWebUtils.readSubmissionAssessment(section, attempt.edgePath, asUser).leftMap(_.to404)
      _       <- validateAttemptInContext(attempt, section.lwc).leftMap(_.to404)
      _       <- validateUserForAttemptOrInstructor(attempt, asUser, courseAccessService).leftMap(_.to404)
    yield (section, attempt)

  private val loadValidatedAttempt          = validatedSectionAndAttempt(submissionAttemptService.fetch)
  private val loadValidatedAttemptForUpdate = validatedSectionAndAttempt(submissionAttemptService.fetchForUpdate)

  private def loadValidatedAssessment(
    contextId: ContextId,
    edgePath: EdgePath,
    asUser: UserId
  ): ErrorResponse \/ (CourseSection, SubmissionAssessment) =
    for
      rights                <-
        courseAccessService.actualRights(contextId, currentUser) \/> ErrorResponse.forbidden
      _                     <- impersonationService.checkImpersonation(contextId, asUser).leftMap(_.getMessage.to404)
      (section, assessment) <- submissionAssessmentWebUtils
                                 .readSubmissionAssessment(contextId, edgePath, asUser, rights.some)
                                 .leftMap(_.to404)
    yield (section, assessment)

  private def loadUserDto(viewAsId: Option[JLong]): ErrorResponse \/ UserDTO =
    viewAsId match
      case None     => currentUser.right
      case Some(id) => userWebUtils.loadUser(id).leftMap(_.to422)

  override def getAttempt(
    attemptId: AttemptId,
    contextId: ContextId,
    viewAsId: Option[JLong] = None
  ): ErrorResponse \/ SubmissionAttemptDto =
    val viewAs: UserId = viewAsId.cata(UserId(_), currentUser)
    for (_, attempt) <- loadValidatedAttempt(attemptId.value, contextId.value, viewAs)
    yield toDto(attempt, attempt.assessment.contentId.contextId)

  override def getAttempts(
    contentId: ContentIdentifier,
    viewAsId: Option[JLong] = None
  ): ErrorResponse \/ Seq[SubmissionAttemptDto] =
    for
      viewAs                <- loadUserDto(viewAsId)
      (section, assessment) <- loadValidatedAssessment(contentId.contextId, contentId.edgePath, viewAs)
    yield toDtos(
      submissionAttemptService.getUserAttempts(section, Seq(assessment), viewAs),
      assessment.contentId.contextId
    )

  override def createAttempt(
    request: AttemptCreationRequest,
    context: ContextId
  ): ErrorResponse \/ SubmissionAttemptDto =
    val activityId: ContentIdentifier = request.contentId

    for
      subject               <- loadUserDto(request.subjectId)
      (section, assessment) <- loadValidatedAssessment(activityId.contextId, activityId.edgePath, currentUser)
      _                     <- assessmentValidationUtils.validatedDueDate(section, assessment, now.instant, currentUser.id).leftMap(_.to422)
      attempt               <- submissionAttemptService.createAttempt(assessment, subject, currentUser).leftMap(_.message.to422)
    yield toDto(attempt, assessment.contentId.contextId)
  end createAttempt

  override def respondToAttempt(
    attemptId: AttemptId,
    response: SubmissionResponseRequest,
    contextId: ContextId
  ): ErrorResponse \/ SubmissionAttemptDto =
    def submitIfRequested(attempt: SubmissionAttempt, submit: Boolean): SubmissionAttemptFailure \/ SubmissionAttempt =
      if submit then submissionAttemptService.submitAttempt(attempt, currentUser)
      else attempt.right

    for
      (section, attempt0) <- loadValidatedAttemptForUpdate(attemptId.value, contextId.value, currentUser)
      _                   <- assessmentValidationUtils
                               .validatedDueDate(section, attempt0.assessment, now.instant, currentUser.id)
                               .leftMap(_.to422)
      attachments         <- toInvalidUploadError(collectAttachments(attempt0, response))
      attempt1            <- submissionAttemptService
                               .respond(attempt0, response.essay, attachments, currentUser)
                               .leftMap(_.message.to422)
      attempt2            <- submitIfRequested(attempt1, response.submit).leftMap(_.message.to422)
    yield toDto(attempt2, contextId)
    end for
  end respondToAttempt

  def toInvalidUploadError[A](value: ValidationNel[String, A]): ErrorResponse \/ A =
    value.toDisjunction.leftMap(errors => ErrorResponse.unprocessable(InvalidUploads(errors).getJson))

  override def submit(attemptId: AttemptId, contextId: ContextId): ErrorResponse \/ SubmissionAttemptDto =
    for
      (section, attempt) <- loadValidatedAttemptForUpdate(attemptId.value, contextId.value, currentUser)
      _                  <- assessmentValidationUtils
                              .validatedDueDate(section, attempt.assessment, now.instant, currentUser.id)
                              .leftMap(_.to422)
      updatedAttempt     <- submissionAttemptService.submitAttempt(attempt, currentUser).leftMap(_.message.to422)
    yield toDto(updatedAttempt, contextId)

  override def score(
    attemptId: AttemptId,
    request: ScoreRequest,
    contextId: ContextId
  ): ErrorResponse \/ SubmissionAttemptDto =
    def reopenIfNecessary(attempt: SubmissionAttempt): ErrorResponse \/ SubmissionAttempt =
      if attempt.state == Finalized then
        submissionAttemptService.reopenAttempt(attempt, currentUser.id).leftMap(_.message.to422)
      else attempt.right

    def submitScoreIfRequested(
      ws: AttachedReadWorkspace,
      section: CourseSection,
      attempt: SubmissionAttempt,
      submit: Boolean
    ): ErrorResponse \/ SubmissionAttempt =
      if submit then submissionAttemptService.submitScore(ws, section, attempt).leftMap(_.message.to422)
      else attempt.right

    for
      (section, attempt0) <- loadValidatedAttemptForUpdate(attemptId.value, contextId.value, currentUser)
      ws                   = courseWorkspaceService.loadReadWorkspace(section)
      attempt1            <- reopenIfNecessary(attempt0)
      attempt2            <- submissionAttemptService.setScore(attempt1, request.score, currentUser).leftMap(_.message.to422)
      attempt3            <- submitScoreIfRequested(ws, section, attempt2, request.submit)
    yield toDto(attempt3, contextId)
  end score

  override def invalidateAttempt(attemptId: AttemptId, contextId: ContextId): ErrorResponse \/ SubmissionAttemptDto =
    for
      (_, attempt)   <- loadValidatedAttemptForUpdate(attemptId.value, contextId.value, currentUser)
      _              <- validateAttemptInContext(attempt, contextId).leftMap(_.to422)
      updatedAttempt <- submissionAttemptService.invalidateAttempt(attempt).leftMap(_.message.to422)
    yield toDto(updatedAttempt, contextId)

  override def feedback(
    attemptId: AttemptId,
    request0: ArgoBody[SubmissionFeedbackRequest],
    contextId: ContextId,
  ): ErrorResponse \/ SubmissionAttemptDto =
    def feedback(
      attempt: SubmissionAttempt,
      feedback: Seq[Feedback],
      submit: Boolean
    ): ErrorResponse \/ SubmissionAttempt =
      if submit then submissionAttemptService.submitFeedback(attempt, feedback).leftMap(_.message.to422)
      else submissionAttemptService.draftFeedback(attempt, feedback).leftMap(_.message.to422)

    for
      request        <- request0.decodeOrMessage.leftMap(_.to422)
      (_, attempt)   <- loadValidatedAttemptForUpdate(attemptId.value, contextId.value, currentUser)
      _              <- validateAttemptInContext(attempt, contextId).leftMap(_.to422)
      _              <- validateIsInstructorlike(contextId, courseAccessService).leftMap(_.to404)
      feedbackValues <- toInvalidUploadError(toQuestionResponseFeedbacks(attempt, request))
      updatedAttempt <- feedback(attempt, feedbackValues, request.submit)
    yield toDto(updatedAttempt, contextId)
  end feedback

  private def toQuestionResponseFeedbacks(
    attempt: SubmissionAttempt,
    feedbackRequest: SubmissionFeedbackRequest,
  ): ValidationNel[String, List[Feedback]] =
    val security = SecuritySettings.config.getDomain
    feedbackRequest.values
      .traverse(toQuestionResponseFeedback(attempt, _, now.instant, submissionAssessmentAttachmentService, security))

  def toDto(attempt: SubmissionAttempt, contextId: ContextId): SubmissionAttemptDto =
    val instructorLike        = courseAccessService.hasInstructorAccess(contextId)
    val attachmentInfos       = loadAttachmentInfos(attempt, instructorLike)
    val alwaysReleaseFeedback = alwaysReleaseFeedbackForContext(contextId)
    SubmissionAttemptDto(attempt, instructorLike, alwaysReleaseFeedback, attachmentInfos)

  def toDtos(attempts: Seq[SubmissionAttempt], contextId: ContextId): Seq[SubmissionAttemptDto] =
    val instructorLike        = courseAccessService.hasInstructorAccess(contextId)
    val alwaysReleaseFeedback = alwaysReleaseFeedbackForContext(contextId)
    attempts map { attempt =>
      val attachmentInfos = loadAttachmentInfos(attempt, instructorLike)
      SubmissionAttemptDto(attempt, instructorLike, alwaysReleaseFeedback, attachmentInfos)
    }

  override def attachment(
    attemptId: AttemptId,
    attachmentId: AttachmentId,
    download: Option[JBoolean],
    direct: Option[JBoolean],
    size: String,
    context: ContextId
  ): ErrorResponse \/ FileResponse[? <: FileInfo] =
    for
      section  <- courseWebUtils.loadCourseSection(context.id).leftMap(_.to404)
      ws        = courseWorkspaceService.loadReadWorkspace(section)
      attempt  <- submissionAssessmentWebUtils.loadAttempt(section, ws, attemptId.value).leftMap(_.message.to404)
      _        <- validateAttemptInContext(attempt, context).leftMap(_.to422)
      _        <- validateUserForAttemptOrInstructor(attempt, currentUser, courseAccessService).leftMap(_.to404)
      response <- submissionAssessmentAttachmentService.buildFileResponse(
                    UserId(attempt.user.id),
                    attemptId,
                    attachmentId,
                    download.isTrue,
                    direct.isTrue,
                    size
                  )
    yield response

  override def attachmentUrl(
    attemptId: AttemptId,
    attachmentId: AttachmentId,
    context: ContextId
  ): ErrorResponse \/ String =
    for
      section  <- courseWebUtils.loadCourseSection(context.id).leftMap(_.to404)
      ws        = courseWorkspaceService.loadReadWorkspace(section)
      attempt  <- submissionAssessmentWebUtils.loadAttempt(section, ws, attemptId.value).leftMap(_.message.to404)
      _        <- validateAttemptInContext(attempt, context).leftMap(_.to422)
      _        <- validateUserForAttemptOrInstructor(attempt, currentUser, courseAccessService).leftMap(_.to404)
      redirect <- submissionAssessmentAttachmentService.buildRedirectUrl(
                    UserId(attempt.user.id),
                    attemptId,
                    attachmentId
                  )
    yield redirect

  private def collectAttachments(
    attempt: SubmissionAttempt,
    request: SubmissionResponseRequest
  ): ValidationNel[String, List[AttachmentId]] =
    val security = SecuritySettings.config.getDomain
    request.uploads.orZ.traverse(SecuritySettings.validateUpload(security)) map { uploadInfos =>
      val newAttachments: List[AttachmentId]      =
        uploadInfos.map(submissionAssessmentAttachmentService.addAttachment(UserId(attempt.user.id), attempt.id, _))
      val existingAttachments: List[AttachmentId] = request.attachments.orZ
      existingAttachments ++ newAttachments
    }
  end collectAttachments

  private def loadAttachmentInfos(attempt: SubmissionAttempt, instructorLike: Boolean): Seq[AttachmentInfo] =
    val feedbackAttachments: Option[Seq[AttachmentId]] =
      Option(attempt.feedback.flatMap(_.attachments))

    submissionAssessmentAttachmentService.loadAttachmentInfos(
      UserId(attempt.user.id),
      attempt.attachments ++ feedbackAttachments.getOrElse(Nil)
    )
end SubmissionAssessmentAttemptWebControllerImpl
