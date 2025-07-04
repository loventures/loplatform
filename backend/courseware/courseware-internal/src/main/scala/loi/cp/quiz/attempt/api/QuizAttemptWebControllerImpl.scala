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

package loi.cp.quiz
package attempt
package api

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.exception.UnprocessableEntityException
import com.learningobjects.cpxp.component.web.{ArgoBody, ErrorResponse, FileResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.service.exception.{
  AccessForbiddenException,
  BusinessRuleViolationException,
  ResourceNotFoundException
}
import com.learningobjects.cpxp.service.user.{UserDTO, UserId, UserType}
import com.learningobjects.cpxp.util.FileInfo
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.assessment.*
import loi.cp.assessment.api.{AssessmentValidationUtils, AttemptValidationUtils, ResponseFeedbackUtils}
import loi.cp.assessment.attempt.AttemptState
import loi.cp.attachment.{AttachmentId, AttachmentInfo, InvalidUploads}
import loi.cp.config.ConfigurationService
import loi.cp.content.{CourseContentService, CourseWebUtils}
import loi.cp.context.ContextId
import loi.cp.course.*
import loi.cp.customisation.CourseCustomisationService
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.quiz.api.QuizWebUtils
import loi.cp.quiz.attachment.QuizAttachmentService
import loi.cp.quiz.attempt.DistractorOrder.{AuthoredResponseSelection, DisplayResponseSelection}
import loi.cp.quiz.attempt.QuizAttemptService.{AttachmentRequest, AutoSubmitGracePeriod, SelectionRequest}
import loi.cp.quiz.attempt.api.QuestionDtoBuilder.buildQuestionDtos
import loi.cp.quiz.attempt.api.QuizAttemptWebController.*
import loi.cp.quiz.question.api.QuestionRationaleDisplay.{All, NoRationale, OnlyCorrect, OnlyIncorrect}
import loi.cp.quiz.question.api.{QuestionDto, QuestionDtoUtils, QuestionRationaleDisplay}
import loi.cp.quiz.question.{Question, QuestionService}
import loi.cp.quiz.settings.ReleaseRemediationCondition
import loi.cp.quiz.settings.ReleaseRemediationCondition.{AnyResponse, OnCorrectResponse}
import loi.cp.reference.*
import loi.cp.security.SecuritySettings
import loi.cp.user.ImpersonationService
import loi.cp.user.web.UserWebUtils
import org.log4s.Logger
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scalaz.{ValidationNel, \/}
import scaloi.misc.TimeSource
import scaloi.misc.TryInstances.*
import scaloi.syntax.boolean.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.map.*
import scaloi.syntax.option.*
import scaloi.syntax.validation.*

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import scala.util.{Success, Try}

@Component
class QuizAttemptWebControllerImpl(
  val componentInstance: ComponentInstance,
)(
  quizWebUtils: QuizWebUtils,
  assessmentValidationUtils: AssessmentValidationUtils,
  courseWebUtils: CourseWebUtils,
  courseAccessService: CourseAccessService,
  courseSectionService: CourseSectionService,
  courseContentService: CourseContentService,
  courseCustomisationService: CourseCustomisationService,
  currentUser: => UserDTO,
  impersonationService: ImpersonationService,
  questionService: QuestionService,
  quizAttachmentService: QuizAttachmentService,
  quizAttemptService: QuizAttemptService,
  courseWorkspaceService: CourseWorkspaceService,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  time: TimeSource,
  userWebUtils: UserWebUtils,
)(implicit configurationService: ConfigurationService)
    extends ComponentImplementation
    with QuizAttemptWebController:
  import AttemptValidationUtils.*
  import QuizAttemptWebControllerImpl.*
  import ResponseFeedbackUtils.*

  private val logger: Logger = org.log4s.getLogger

  private def viewingAs(viewAsId: Option[JLong]): UserId = viewAsId.cata(UserId(_), currentUser)

  private def validateAttempt(loadAttempt: (CourseSection, Long, List[CourseAssessmentPolicy]) => Option[QuizAttempt])(
    attemptId: Long,
    contextId: Long,
    asUser: UserId
  ): Try[(CourseSection, QuizAttempt)] =
    for
      section <- courseSectionService.getCourseSection(contextId).elseFailure(new ResourceNotFoundException(""))
      policies = courseAssessmentPoliciesService.getPolicies(section)
      _       <- impersonationService.checkImpersonation(section, asUser).toTry
      attempt <- loadAttempt(section, attemptId, policies).toTry(new ResourceNotFoundException(""))
      _       <- validateUserForAttemptOrInstructor(attempt, asUser, courseAccessService).toTry(
                   new ResourceNotFoundException(_)
                 )
    yield (section, attempt)

  private val loadValidatedAttempt          =
    validateAttempt((section, attemptId, policies) => quizAttemptService.fetch(section, attemptId, policies))
  private val loadValidatedAttemptForUpdate =
    validateAttempt((section, attemptId, policies) => quizAttemptService.fetchForUpdate(section, attemptId, policies))

  private def loadValidatedQuiz(contextId: Long, edgePath: EdgePath, asUser: UserId): Try[(CourseSection, Quiz)] =
    for
      section <- courseSectionService.getCourseSection(contextId).elseFailure(new ResourceNotFoundException(""))
      _       <- impersonationService.checkImpersonation(section, asUser).toTry
      quiz    <- quizWebUtils.readQuiz(section, edgePath).toTry(new ResourceNotFoundException(_))
    yield (section, quiz)

  private def loadUserDto(viewAsId: Option[JLong]): Try[UserDTO] =
    viewAsId match
      case None     => Success(currentUser)
      case Some(id) => userWebUtils.loadUser(id).toTry(msg => new UnprocessableEntityException(msg))

  override def getAttempt(
    attemptId: AttemptId,
    contextId: ContextId,
    viewAsId: Option[JLong] = None
  ): Try[QuizAttemptDto] =
    for
      (_, attempt)         <- loadValidatedAttempt(attemptId.value, contextId.value, viewingAs(viewAsId))
      instructorLike        = courseAccessService.hasInstructorAccess(attempt.contentId.contextId)
      alwaysReleaseFeedback = configurationService.getItem(CoursePreferences)(contextId).alwaysReleaseFeedback
      attachments           = getVisibleAttachmentInfos(attempt, instructorLike, alwaysReleaseFeedback)
    yield toDto(attempt, attachments)

  override def getAttempts(identifier: ContentIdentifier, viewAsId: Option[JLong] = None): Try[Seq[QuizAttemptDto]] =
    for
      viewAs               <- loadUserDto(viewAsId)
      (section, quiz)      <- loadValidatedQuiz(identifier.contextId.value, identifier.edgePath, viewAs)
      attempts              = quizAttemptService.getUserAttempts(section, Seq(quiz), viewAs)
      instructorLike        = courseAccessService.hasInstructorAccess(identifier.contextId)
      alwaysReleaseFeedback = configurationService
                                .getItem(CoursePreferences)(identifier.contextId)
                                .alwaysReleaseFeedback
      attachmentsByAttempt  = getVisibleAttachmentInfos(viewAs, attempts, instructorLike, alwaysReleaseFeedback)
    yield toDtos(quiz, attempts, attachmentsByAttempt)

  override def createAttempt(request: AttemptCreationRequest, context: ContextId): Try[QuizAttemptDto] =
    val activityId: ContentIdentifier = request.contentId
    for
      (section, quiz) <- loadValidatedQuiz(context.value, request.contentId.edgePath, currentUser)
      ws               = courseWorkspaceService.loadReadWorkspace(section)
      _               <- assessmentValidationUtils
                           .validatedDueDate(section, quiz, time.instant, currentUser.id)
                           .toTry(new BusinessRuleViolationException(_))
      unassessables    = courseContentService.findUnassessables(ws, section)
      customisation    = courseCustomisationService.loadCustomisation(section.lwc)
      questions        = questionService.pickQuestions(quiz, unassessables, customisation, ws, request.competencies)
      attempt         <- quizAttemptService
                           .createAttempt(quiz, questions, currentUser)
                           .toTry(f => new RuntimeException(f.message))
    yield toDto(attempt, Nil)
    end for
  end createAttempt

  override def respond(
    attemptId: AttemptId,
    responses: RespondToAttemptDto,
    context: ContextId
  ): Try[QuizAttemptDto] =
    for
      (section, attempt)   <- loadValidatedAttemptForUpdate(attemptId.value, context.value, currentUser)
      ws                    = courseWorkspaceService.loadReadWorkspace(section)
      _                     = AssessmentLogMeta.attemptMetadata(attempt)
      _                    <- assessmentValidationUtils
                                .validatedDueDate(section, attempt.assessment, time.instant, currentUser.id)
                                .toTry(new BusinessRuleViolationException(_))
      _                    <- validateTimeLimit(attempt).elseFailure(new BusinessRuleViolationException("Time limit exceeded"))
      selections           <- parseRequestSelections(attempt, responses)
      attachments          <- parseAttachments(attempt, responses).toTry(InvalidUploads.apply)
      updated              <- quizAttemptService
                                .respond(
                                  ws,
                                  section,
                                  attempt,
                                  selections,
                                  attachments,
                                  responses.submit.isTrue,
                                  responses.autoSubmit.isTrue
                                )
                                .mapToRestException
      instructorLike        = courseAccessService.hasInstructorAccess(updated.contentId.contextId)
      alwaysReleaseFeedback = configurationService
                                .getItem(CoursePreferences)(updated.contentId.contextId)
                                .alwaysReleaseFeedback
      attachments           = getVisibleAttachmentInfos(updated, instructorLike, alwaysReleaseFeedback)
    yield toDto(updated, attachments)

  override def submit(attemptId: AttemptId, context: ContextId): Try[QuizAttemptDto] =
    respond(attemptId, RespondToAttemptDto(Nil, Some(true), None), context)

  // validate that this is within the time limit, allowing a grade period
  private def validateTimeLimit(attempt: QuizAttempt): Boolean =
    attempt.maxMinutes.forall(limit =>
      time.instant.getEpochSecond - attempt.createTime.getEpochSecond <= limit * 60L + AutoSubmitGracePeriod.toSeconds
    )

  override def testOut(attemptId: AttemptId, context: ContextId): Try[List[EdgePath]] =
    for
      (section, attempt) <- loadValidatedAttempt(attemptId.value, context.value, currentUser)
      ws                  = courseWorkspaceService.loadReadWorkspace(section)
      _                   = AssessmentLogMeta.attemptMetadata(attempt)
      _                  <- attempt.assessment.isDiagnostic <@~* new BusinessRuleViolationException("not a diagnostic")
      _                  <- (attempt.state == AttemptState.Finalized) <@~* new BusinessRuleViolationException("not finalized")
      testedOut          <- quizAttemptService.testOut(ws, section, attempt).mapToRestException
    yield testedOut

  private def parseRequestSelections(
    attempt: QuizAttempt,
    requests: RespondToAttemptDto
  ): Try[Seq[SelectionRequest]] =
    requests.responses.traverse[Try, SelectionRequest] { req =>
      selectionsInAuthoredOrder(req.selection, attempt.questions(req.questionIndex))
        .map(sel => SelectionRequest(req.questionIndex, sel, req.submitResponse))
    }

  private def selectionsInAuthoredOrder(
    maybeSelection: Option[DisplayResponseSelection],
    question: QuizAttemptQuestionUsage
  ): Try[Option[AuthoredResponseSelection]] =
    question.distractorOrder match
      case Some(distractorOrder) => maybeSelection.map(distractorOrder.toAuthoredOrder).sequence
      case None                  => maybeSelection.map(AuthoredOrder.instance.toAuthoredOrder).sequence

  def parseAttachments(
    attempt: QuizAttempt,
    requests: RespondToAttemptDto
  ): ValidationNel[String, List[AttachmentRequest]] =
    val security                                                                            = SecuritySettings.config.getDomain
    def parseRequest(request: QuestionSelectionDto): String ValidationNel AttachmentRequest =
      request.uploads.orZ.traverse(SecuritySettings.validateUpload(security)) map { uploadInfos =>
        val newAttachmentIds: List[AttachmentId]    =
          uploadInfos.map(quizAttachmentService.addAttachment(UserId(attempt.user.id), attempt.id, _))
        val existingAttachments: List[AttachmentId] = request.attachments.orZ.map(AttachmentId.apply)
        AttachmentRequest(request.questionIndex, existingAttachments ++ newAttachmentIds)
      }
    requests.responses.traverse(parseRequest)
  end parseAttachments

  override def feedback(
    attemptId: AttemptId,
    feedbackRequest0: ArgoBody[QuestionResponseFeedbackRequest],
    context: ContextId
  ): Try[QuizAttemptDto] =

    for
      (section, attempt)   <- loadValidatedAttemptForUpdate(attemptId.value, context.value, currentUser)
      _                     = AssessmentLogMeta.attemptMetadata(attempt)
      feedbackRequest      <- feedbackRequest0.decode_!
      responseFeedback     <- toQuestionResponseFeedbacks(attempt, feedbackRequest)
      updatedAttempt       <-
        quizAttemptService
          .setResponseFeedback(attempt, feedbackRequest.questionIndex, responseFeedback, feedbackRequest.submit)
          .mapToRestException
      instructorLike        = courseAccessService.hasInstructorAccess(updatedAttempt.contentId.contextId)
      alwaysReleaseFeedback = configurationService
                                .getItem(CoursePreferences)(updatedAttempt.contentId.contextId)
                                .alwaysReleaseFeedback
      attachments           = getVisibleAttachmentInfos(updatedAttempt, instructorLike, alwaysReleaseFeedback)
    yield toDto(updatedAttempt, attachments)

  private def toQuestionResponseFeedbacks(
    attempt: QuizAttempt,
    feedbackRequest: QuestionResponseFeedbackRequest,
  ): Try[List[Feedback]] =
    val security = SecuritySettings.config.getDomain
    feedbackRequest.values
      .traverse(toQuestionResponseFeedback(attempt, _, time.instant, quizAttachmentService, security))
      .toTry(InvalidUploads.apply)

  override def attachment(
    attemptId: AttemptId,
    attachmentId: AttachmentId,
    download: Option[JBoolean],
    direct: Option[JBoolean],
    size: String,
    contextId: ContextId
  ): ErrorResponse \/ FileResponse[? <: FileInfo] =
    for
      section <- courseWebUtils.loadCourseSection(contextId.value).leftMap(_.to404)
      policies = courseAssessmentPoliciesService.getPolicies(section)
      attempt <- quizAttemptService
                   .fetch(section, attemptId.value, policies)
                   .toRightDisjunction(ErrorResponse.notFound)
      _       <- validateUserForAttemptOrInstructor(attempt, currentUser, courseAccessService).leftMap(_.to404)
      _       <- validateAttachmentAccess(attempt, attachmentId)
      result  <- quizAttachmentService.buildFileResponse(
                   UserId(attempt.user.id),
                   attemptId,
                   attachmentId,
                   download.isTrue,
                   direct.isTrue,
                   size
                 )
    yield result

  override def invalidateAttempt(attemptId: AttemptId, context: ContextId): Try[QuizAttemptDto] =
    for
      (_, priorAttempt) <- loadValidatedAttemptForUpdate(attemptId.value, context.value, currentUser)
      instructorLike     = courseAccessService.hasInstructorAccess(context)
      _                 <- (instructorLike || currentUser.userType == UserType.Preview) <@~* new AccessForbiddenException()
      _                  = AssessmentLogMeta.attemptMetadata(priorAttempt)
      attempt           <- quizAttemptService.invalidateAttempt(priorAttempt).mapToRestException
      attachments        = getVisibleAttachmentInfos(attempt, instructorLike, alwaysReleaseFeedback = false)
    yield toDto(attempt, attachments)

  override def scoreResponse(
    attemptId: AttemptId,
    context: ContextId,
    scoringRequest: ResponseScoringRequest
  ): Try[QuizAttemptDto] =

    def scoreAttempt(
      ws: AttachedReadWorkspace,
      section: CourseSection,
      attempt: QuizAttempt
    ): QuizAttemptFailure \/ QuizAttempt =
      if scoringRequest.submit then
        quizAttemptService.submitResponseScore(
          ws,
          section,
          attempt,
          scoringRequest.questionIndex,
          scoringRequest.score,
          Some(currentUser.id)
        )
      else
        quizAttemptService.draftResponseScore(
          attempt,
          scoringRequest.questionIndex,
          Option(scoringRequest.score),
          Some(currentUser.id)
        )

    for
      (section, attempt) <- loadValidatedAttemptForUpdate(attemptId.value, context.value, currentUser)
      ws                  = courseWorkspaceService.loadReadWorkspace(section)
      _                   = AssessmentLogMeta.attemptMetadata(attempt)
      scoredAttempt      <- scoreAttempt(ws, section, attempt).mapToRestException
      attachments         = getVisibleAttachmentInfos(scoredAttempt, isInstructorlike = true, alwaysReleaseFeedback = false)
    yield toDto(scoredAttempt, attachments)
  end scoreResponse

  private def toDto(attempt: QuizAttempt, attachments: Seq[AttachmentInfo]): QuizAttemptDto =
    toDtos(attempt.assessment, Seq(attempt), Map(attempt -> attachments)).head

  /** Bulk loads content and passes off to dedicated DTO building code.
    *
    * @param quiz
    *   the quiz for the attempts
    * @param attempts
    *   the attempts to marshal into DTOs
    * @param attachmentsByAttempt
    *   a map of all attachments used by the given attempt
    * @return
    *   REST objects for all the given attempts with appropriate data filtering for the user
    */
  private def toDtos(
    quiz: Quiz,
    attempts: Seq[QuizAttempt],
    attachmentsByAttempt: Map[QuizAttempt, Seq[AttachmentInfo]]
  ): Seq[QuizAttemptDto] =
    val references: Seq[VersionedAssetReference]              = attempts.flatMap(_.questions).map(_.questionPointer)
    val questions: Seq[Question]                              = questionService.getQuestions(quiz.section, references)
    val questionByRef: Map[VersionedAssetReference, Question] =
      questions.map(question => question.contentReference -> question).toMap

    val contextIds: Seq[ContextId]                               = attempts.map(_.contentId.contextId).distinct
    val isInstructorlikeForContext: Map[ContextId, Boolean]      =
      contextIds.map(contextId => contextId -> courseAccessService.hasInstructorAccess(contextId)).toMap
    val alwaysReleaseFeedbackForContext: Map[ContextId, Boolean] =
      contextIds
        .map(contextId => contextId -> configurationService.getItem(CoursePreferences)(contextId).alwaysReleaseFeedback)
        .toMap

    attempts.map(attempt =>
      val attemptQuestionReferences: Seq[VersionedAssetReference]  = attempt.questions.map(_.questionPointer)
      val attemptQuestions: Seq[Question]                          = attemptQuestionReferences.map(questionByRef)
      val attemptAttachments: Seq[AttachmentInfo]                  = attachmentsByAttempt(attempt)
      val isInstructorlike: Boolean                                = isInstructorlikeForContext(attempt.contentId.contextId)
      val alwaysReleaseFeedback: Boolean                           = alwaysReleaseFeedbackForContext(attempt.contentId.contextId)
      val remediationReleaseCondition: ReleaseRemediationCondition =
        quiz.settings.resultsPolicy.remediationReleaseCondition

      QuizAttemptDtoBuilder
        .buildAttemptDto(
          attempt,
          remediationReleaseCondition,
          attemptQuestions,
          attemptAttachments,
          isInstructorlike,
          alwaysReleaseFeedback,
          time.instant,
        )
    )
  end toDtos

  private def validateAttachmentAccess(attempt: QuizAttempt, attachmentId: AttachmentId): ErrorResponse \/ Unit =
    val isResponseAttachment: Boolean =
      attempt.responses.exists(response => response.attachments.contains(attachmentId))

    val responsesWithFeedbackAttachment: Seq[QuizQuestionResponse] =
      attempt.responses.filter(response => response.instructorFeedback.exists(_.attachments.contains(attachmentId)))

    if isResponseAttachment || responsesWithFeedbackAttachment.nonEmpty then
      // Is any response with this attachment in feedback released
      val attachmentReleased: Boolean    = responsesWithFeedbackAttachment.exists(_.instructorFeedbackReleased)
      val isInstructorlike: Boolean      = courseAccessService.hasInstructorAccess(attempt.contentId.contextId)
      val alwaysReleaseFeedback: Boolean =
        configurationService.getItem(CoursePreferences)(attempt.contentId.contextId).alwaysReleaseFeedback

      if isResponseAttachment || isInstructorlike || attachmentReleased || alwaysReleaseFeedback then ().right
      else ErrorResponse.forbidden("You do not have permission to view this attachment").left
    else ErrorResponse.notFound(s"No attachment found for $attachmentId in attempt ${attempt.id}").left
    end if
  end validateAttachmentAccess

  private def getVisibleAttachmentInfos(
    attempt: QuizAttempt,
    isInstructorlike: Boolean,
    alwaysReleaseFeedback: Boolean
  ): Seq[AttachmentInfo] =
    getVisibleAttachmentInfos(UserId(attempt.user.id), Seq(attempt), isInstructorlike, alwaysReleaseFeedback)
      .getOrElse(attempt, Nil)

  /** Returns all attachments for a single user's set of attempts. This method does not support viewing multiple users
    * attempts.
    *
    * @param userId
    *   the owner of the attempt
    * @param attempts
    *   the attempts to get attachment infos for
    * @param isInstructorlike
    *   whether the requester is priviliged
    * @return
    *   the attachment infos for the given attempts
    */
  private def getVisibleAttachmentInfos(
    userId: UserId,
    attempts: Seq[QuizAttempt],
    isInstructorlike: Boolean,
    alwaysReleaseFeedback: Boolean
  ): Map[QuizAttempt, Seq[AttachmentInfo]] =
    if !attempts.forall(_.user.id == userId.value) then
      throw new IllegalArgumentException("Attempting to view multiple users attempts")

    val attachmentIdsByAttempt: Map[QuizAttempt, Seq[AttachmentId]] =
      (for attempt <- attempts
      yield
        val visibleAttachmentIds: Seq[AttachmentId] =
          attempt.responses.flatMap(response =>
            response.attachments ++ ifFeedbackIsVisible(
              response,
              response.instructorFeedbackReleased,
              isInstructorlike,
              alwaysReleaseFeedback,
            )(_.instructorFeedback.flatMap(_.attachments)).getOrElse(Nil)
          )

        attempt -> visibleAttachmentIds
      ).toMap

    val attachmentInfos: Seq[AttachmentInfo]                  =
      quizAttachmentService.loadAttachmentInfos(userId, attachmentIdsByAttempt.values.flatten.toSeq)
    val attachmentInfoById: Map[AttachmentId, AttachmentInfo] = attachmentInfos.map(info => info.id -> info).toMap

    attachmentIdsByAttempt.mapValuesEagerly(attachmentIds => attachmentIds.map(attachmentInfoById))
  end getVisibleAttachmentInfos

  /** Returns the results of {{f}} applied to {{response}} if the current user can see feedback for the response.
    *
    * @param response
    *   the response in question
    * @param feedbackReleased
    *   the state of whether feedback is released to the learner
    * @param isInstructorlike
    *   whether the current user is a superuser
    * @param f
    *   the transform to apply to the response
    * @tparam F
    *   the type of response
    * @tparam T
    *   the transformed type
    * @return
    *   the resulting Some[T] from {{f}} if the current user can see feedback; otherwise, [[None]]
    */
  def ifFeedbackIsVisible[F, T](
    response: F,
    feedbackReleased: Boolean,
    isInstructorlike: Boolean,
    alwaysReleaseFeedback: Boolean
  )(
    f: F => T
  ): Option[T] =
    if isInstructorlike || feedbackReleased || alwaysReleaseFeedback then Some(f(response))
    else None
end QuizAttemptWebControllerImpl

object QuizAttemptWebControllerImpl:
  implicit class QuizAttemptResult[A](val result: QuizAttemptFailure \/ A) extends AnyVal:
    def mapToRestException: Try[A] = result.toTry(f => toException(f))

  def toException(failure: QuizAttemptFailure): Exception =
    failure match
      // Can't do this right now
      case _: AttemptLimitExceeded | _: ReopeningAttemptFailure | MissingResponseScore |
          InvalidatedAttemptScoringFailure | _: MissingResponseScoresFailure | _: QuizAttemptResponseStateFailure |
          _: AlreadyInvalidFailure | _: NotYetScored =>
        new BusinessRuleViolationException(failure.message)

      // Your request doesn't make sense
      case _: QuizRubricScoringFailure | _: MismatchedResponseType | _: IllegalUploadTarget | _: TooManySelections |
          InvalidSelectionIndex | _: QuestionIndexOutOfBounds =>
        new UnprocessableEntityException(failure.message)
end QuizAttemptWebControllerImpl

private object QuizAttemptDtoBuilder:
  import ResponseDtoBuilder.*

  /** Build attempt DTOs for the given attempt, for either an instructor/superuser or learner. This will filter details
    * to the appropriate level based on user access, attempt state and quiz configuration.
    *
    * @param attempt
    *   the attempt to process
    * @param remediationReleaseCondition
    *   the quiz configuration for the attempt describing when to release remediation
    * @param attemptQuestions
    *   the questions referenced by this attempt
    * @param attachments
    *   all attachments referenced by responses mapped by id
    * @param isInstructorlike
    *   whether the user is a superuser in the context of the attempt
    * @return
    *   the REST object for attempt with appropriate data filtering for the user
    */
  def buildAttemptDto(
    attempt: QuizAttempt,
    remediationReleaseCondition: ReleaseRemediationCondition,
    attemptQuestions: Seq[Question],
    attachments: Seq[AttachmentInfo],
    isInstructorlike: Boolean,
    alwaysReleaseFeedback: Boolean,
    now: Instant = Instant.now,
  ): QuizAttemptDto =
    val attemptQuestionDtos: Seq[QuestionDto]  =
      buildQuestionDtos(attempt, remediationReleaseCondition, attemptQuestions, isInstructorlike)
    val responseDtos: Seq[QuestionResponseDto] = buildResponseDtos(attempt, isInstructorlike, alwaysReleaseFeedback)

    QuizAttemptDto(
      id = attempt.id,
      questions = attemptQuestionDtos,
      responses = responseDtos,
      contentId = attempt.contentId,
      state = attempt.state,
      createTime = attempt.createTime,
      submitTime = attempt.submitTime,
      autoSubmitted = attempt.autoSubmitted,
      remainingMillis = attempt.maxMinutes
        .map(Duration.ofMinutes)
        .map(attempt.createTime.plus)
        .map(attempt.submitTime.getOrElse(now).until(_, ChronoUnit.MILLIS)),
      score = attempt.score.when(
        attempt.state == AttemptState.Finalized
      ), // TODO: Figure out if instructors should always see this
      valid = attempt.valid,
      attachments = attachments.map(attachment => attachment.id.value -> attachment).toMap,
    )
  end buildAttemptDto
end QuizAttemptDtoBuilder

private object ResponseDtoBuilder:

  /** Builds a REST objects for the responses of a given attempt, filtering based on the user and state.
    *
    * @param attempt
    *   the attempt to build response DTOs for
    * @param isInstructorlike
    *   whether the user is a superuser
    * @return
    *   a filtered REST object for the given user type given the state of the attempt
    */
  def buildResponseDtos(
    attempt: QuizAttempt,
    isInstructorlike: Boolean,
    alwaysReleaseFeedback: Boolean
  ): Seq[QuestionResponseDto] =
    val responseDtos: Seq[QuestionResponseDto] = responsesInDisplayOrderOrThrow(attempt)

    if isInstructorlike then responseDtos
    else filterScoreAndFeedbackForStudent(responseDtos, alwaysReleaseFeedback)

  /** Returns unfiltered DTOs of the responses for the given attempt with response selections converted to the display
    * order of the response.
    *
    * That is, if you have a multiple choice question authored with: <p>[A, B, C]</p> A distractor order for the
    * response to that question of: <p>[1, 2, 0] which is [C, A, B]<p/> And an authored order response of: <p>[0] which
    * is [A] in authored order</p> Then the resulting object will have the selection of: <p>[1] which is [A] in display
    * order</p>
    *
    * @param attempt
    *   the attempt to build REST responses for
    * @return
    *   REST objects for the responses of the attempt
    * @throws InvalidSelectionOrderingException
    *   if a response contains a selection does not match the distractor order of the response
    */
  private def responsesInDisplayOrderOrThrow(attempt: QuizAttempt): Seq[QuestionResponseDto] =
    attempt.responses
      .zip(attempt.questions.map(_.distractorOrder))
      .toList
      .traverse[Try, QuestionResponseDto]({ case (response, maybeDistractorOrder) =>
        val order: DistractorOrder = maybeDistractorOrder.getOrElse(AuthoredOrder.instance)
        QuestionResponseDto.of(response, order)
      })
      .get
    // The question and response are on the same object from the same row of the database.  If that somehow does
    // not agree with itself, that is not a recoverable error.

  private def filterScoreAndFeedbackForStudent(
    responses: Seq[QuestionResponseDto],
    alwaysReleaseFeedback: Boolean
  ): Seq[QuestionResponseDto] =
    for response <- responses
    yield
      val filteredScore: Option[ResponseScore] = response.score.when(response.state.scoreReleased)

      val filteredFeedback: Seq[Feedback] =
        Option(response.instructorFeedback)
          .when(response.instructorFeedbackReleased || alwaysReleaseFeedback)
          .getOrElse(Nil)

      response.copy(score = filteredScore, instructorFeedback = filteredFeedback)
end ResponseDtoBuilder

private object QuestionDtoBuilder:
  import QuestionDtoUtils.*

  /** Build question DTOs for the given attempt, for either an instructor/superuser or learner. This will filter
    * question details to the appropriate level based on user access, attempt state and quiz configuration.
    *
    * @param attempt
    *   the attempt referencing the questions
    * @param remediationCondition
    *   the configuration for what conditions must be meet for learners to see remediation
    * @param questions
    *   the questions to process
    * @param isInstructorlike
    *   whether the user is a superuser in the context of the attempt
    * @return
    *   the filtered REST objects for the attempt questions
    */
  def buildQuestionDtos(
    attempt: QuizAttempt,
    remediationCondition: ReleaseRemediationCondition,
    questions: Seq[Question],
    isInstructorlike: Boolean
  ): Seq[QuestionDto] =
    val questionDistractorOrderByRef: Map[VersionedAssetReference, DistractorOrder] =
      attempt.questions.map(q => q.questionPointer -> q.distractorOrder.getOrElse(AuthoredOrder.instance)).toMap

    for (index, question) <- questions.indices zip questions
    yield
      val response                   = attempt.responses(index)
      val showCorrect                = isInstructorlike || showLearnerCorrectAnswer(attempt, remediationCondition, response)
      val isCorrect: Boolean         = response.score.exists(_.isCorrect)
      val includedQuestionRationale  = questionRationaleToInclude(isInstructorlike, showCorrect, isCorrect)
      // You can see all rationale for distractors if you can see any rationale
      val includeDistractorRationale = includedQuestionRationale != QuestionRationaleDisplay.NoRationale
      question.toDto(
        showCorrect,
        includedQuestionRationale,
        includeDistractorRationale,
        questionDistractorOrderByRef(question.contentReference)
      )
    end for
  end buildQuestionDtos

  /** Business rules that define what rationale should be shown, depending on the request.
    *
    * @param isInstructorlike
    *   whether the user in question is a superuser
    * @param showCorrectAnswer
    *   whether the current user is allowed to see the correct answer
    * @param isCorrect
    *   whether this is for a response that is correct
    */
  private def questionRationaleToInclude(
    isInstructorlike: Boolean,
    showCorrectAnswer: Boolean,
    isCorrect: Boolean
  ): QuestionRationaleDisplay =
    if isInstructorlike then All
    else if showCorrectAnswer then
      if isCorrect then OnlyCorrect
      else OnlyIncorrect
    else NoRationale

  /** Returns whether the correct answer should be shown for the question of the given [[QuizQuestionResponse]].
    * (Instructors may always view the correct answer)
    *
    * A learner may the correct answer if the learner has submitted this attempt and: <ol> <li>the quiz is configured to
    * always release remediation</li> <li>the quiz is configured to release remediation on correct responses, and the
    * learner's response is correct</li> </ol>
    *
    * @param attempt
    *   the attempt in question
    * @param remediationCondition
    *   the quiz configuration for the attempt describing when to release remediation
    * @param response
    *   the response in question
    * @return
    *   whether the learner can view the correct answer for the question for {{response}}
    */
  private def showLearnerCorrectAnswer(
    attempt: QuizAttempt,
    remediationCondition: ReleaseRemediationCondition,
    response: QuizQuestionResponse
  ): Boolean =
    if response.state.scoreReleased then
      // Learners can see the correct answer/remediation whenever they could see the score, unless quiz configuration
      // intervene
      remediationCondition match
        case AnyResponse       => true
        case OnCorrectResponse => response.score.exists(_.isCorrect)
    else false
end QuestionDtoBuilder
