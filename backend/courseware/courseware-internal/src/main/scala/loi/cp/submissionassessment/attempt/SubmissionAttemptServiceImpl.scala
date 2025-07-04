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

package loi.cp.submissionassessment.attempt

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.submissionassessment.{SubmissionAttemptEntity, SubmissionAttemptFolderEntity}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.asset.assessment.model.AssessmentType
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.assessment.InstructorOverviews.InstructorAttemptsOverview
import loi.cp.assessment.LearnerOverviews.LearnerAttemptOverview
import loi.cp.assessment.*
import loi.cp.assessment.attempt.*
import loi.cp.assessment.settings.Limited
import loi.cp.attachment.AttachmentId
import loi.cp.context.ContextId
import loi.cp.course.{CourseAccessService, CourseSection}
import loi.cp.mastery.MasteryService
import loi.cp.reference.EdgePath
import loi.cp.submissionassessment.*
import loi.cp.submissionassessment.attempt.actions.*
import loi.cp.submissionassessment.attempt.event.{
  AttemptFinalizedEvent,
  AttemptSubmittedEvent,
  SubmissionAssessmentEventDispatchService,
  SubmissionAttemptEvent
}
import loi.cp.submissionassessment.persistence.{SubmissionAttemptDao, SubmissionAttemptFolderDao}
import loi.cp.submissionassessment.settings.SubmissionAssessmentDriver.{Observation, SubjectDriven}
import loi.cp.user.UserService
import org.log4s.Logger
import scalaz.\/
import scalaz.std.anyVal.*
import scalaz.std.list.*
import scalaz.std.map.*
import scalaz.syntax.either.*
import scalaz.syntax.traverse.*
import scaloi.misc.TimeSource
import scaloi.syntax.collection.*

/** The default implementation of [[SubmissionAttemptService]].
  */
@Service
class SubmissionAttemptServiceImpl(
  courseAccessService: CourseAccessService,
  coursewareAnalyticsService: CoursewareAnalyticsService,
  submissionAttemptDao: SubmissionAttemptDao,
  submissionAttemptFolderDao: SubmissionAttemptFolderDao,
  submissionAttemptLoadService: SubmissionAttemptLoadService,
  submissionAssessmentService: SubmissionAssessmentService,
  submissionAssessmentEventDispatchService: SubmissionAssessmentEventDispatchService,
  ts: TimeSource,
  domainDto: => DomainDTO,
  userService: UserService,
  masteryService: MasteryService,
) extends SubmissionAttemptService:
  import SubmissionAssessmentAttemptOps.*
  import SubmissionAttemptServiceImpl.logger

  override def getAttempts(course: CourseSection, assessments: Seq[SubmissionAssessment]): Seq[SubmissionAttempt] =
    val edgePaths                              = assessments.validateContextOrThrow(course)
    val entities: Seq[SubmissionAttemptEntity] = submissionAttemptDao.getAttempts(course.id, edgePaths)

    val assessmentByPath = assessments.groupUniqBy(_.edgePath)
    val users            = userService.getUsers(entities.map(_.userId))

    for
      entity <- entities
      user   <- users.get(entity.userId)
    yield
      val assessment = assessmentByPath(EdgePath.parse(entity.edgePath))
      entity.toAttempt(assessment, user)
  end getAttempts

  override def getUserAttempts(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment],
    user: UserDTO
  ): Seq[SubmissionAttempt] =
    submissionAttemptLoadService.getUserAttempts(course, assessments, user)

  override def countValidAttempts(course: CourseSection, submissionAssessments: Seq[SubmissionAssessment]): Int =
    submissionAttemptDao.countValidAttempts(course.id, submissionAssessments.map(_.edgePath))

  override def countValidAttempts(
    course: CourseSection,
    submissionAssessment: SubmissionAssessment,
    userId: UserId
  ): Int =
    submissionAttemptLoadService.countValidAttempts(course, submissionAssessment, userId)

  override def createAttempt(
    assessment: SubmissionAssessment,
    subject: UserDTO,
    driver: UserId
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    def hasMoreAttempts: AttemptLimitExceeded \/ Unit =
      val validAttemptCount: Long =
        submissionAttemptDao
          .getUserAttempts(assessment.contextId.value, Seq(assessment.edgePath), subject.id)
          .count(_.valid)

      (validAttemptCount, assessment.settings.maxAttempts) match
        case (attemptCount, Limited(maxAttempts)) if attemptCount >= maxAttempts =>
          AttemptLimitExceeded(maxAttempts).left
        case _                                                                   => ().right
    end hasMoreAttempts

    val selfDriven: Boolean     = subject.id.equals(driver.id)
    val instructorLike: Boolean = courseAccessService.hasInstructorAccess(assessment.contentId.contextId)
    for
      _ <- validateDriver(assessment, selfDriven, instructorLike).widenl
      _ <- hasMoreAttempts
    yield
      val folder: SubmissionAttemptFolderEntity = submissionAttemptFolderDao.getOrCreateAttemptFolder(subject)
      val entity: SubmissionAttemptEntity       =
        submissionAttemptDao.newAttempt(assessment, subject, ts.instant, folder, domainDto)
      val attempt: SubmissionAttempt            = entity.toAttempt(assessment, subject)

      AssessmentLogMeta.attemptMetadata(attempt)
      logger.info(s"New submission attempt created by ${driver.value}")

      attempt
    end for
  end createAttempt

  override def respond(
    attempt: SubmissionAttempt,
    essay: Option[String],
    attachments: Seq[AttachmentId],
    driver: UserId
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    performAsDriver(attempt, driver, Respond(essay, attachments))

  override def submitAttempt(
    attempt: SubmissionAttempt,
    driver: UserId
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    performAsDriver(attempt, driver, SubmitAttempt())

  override def invalidateAttempt(attempt: SubmissionAttempt): SubmissionAttemptFailure \/ SubmissionAttempt =
    perform(attempt, InvalidateAttempt())

  override def setScore(
    attempt: SubmissionAttempt,
    score: Option[ResponseScore],
    scorer: UserDTO
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    perform(attempt, ScoreAttempt(score, scorer.id))

  override def submitScore(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: SubmissionAttempt
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    for newAttempt <- perform(attempt, SubmitAttemptScore())
    yield
      if attempt.assessment.assessmentType == AssessmentType.Summative then
        masteryService.updateUserMasteryForSubmissionAttempt(ws, section, newAttempt)
      newAttempt

  override def reopenAttempt(
    attempt: SubmissionAttempt,
    scorer: Long
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    perform(attempt, ReopenAttempt(scorer))

  override def draftFeedback(
    attempt: SubmissionAttempt,
    feedback: Seq[Feedback]
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    perform(attempt, DraftFeedback(feedback))

  override def submitFeedback(
    attempt: SubmissionAttempt,
    feedback: Seq[Feedback]
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    perform(attempt, SubmitFeedback(feedback))

  override def fetch(
    context: CourseSection,
    ws: AttachedReadWorkspace,
    attemptId: Long,
    policies: List[CourseAssessmentPolicy]
  ): Option[SubmissionAttempt] = fetch(context, ws, attemptId, lock = false, policies)

  override def fetchForUpdate(
    context: CourseSection,
    ws: AttachedReadWorkspace,
    attemptId: Long,
    policies: List[CourseAssessmentPolicy]
  ): Option[SubmissionAttempt] = fetch(context, ws, attemptId, lock = true, policies)

  private def fetch(
    context: CourseSection,
    ws: AttachedReadWorkspace,
    attemptId: Long,
    lock: Boolean,
    policies: List[CourseAssessmentPolicy]
  ): Option[SubmissionAttempt] =
    for
      attemptEntity <- submissionAttemptDao.load(AttemptId(attemptId), lock)
      content       <- context.contents.get(EdgePath.parse(attemptEntity.edgePath))
      if attemptEntity.contextId == context.id
      user          <- userService.getUser(attemptEntity.userId)
    yield
      val assessment =
        submissionAssessmentService
          .getSubmissionAssessment(context, content, policies, ws)
          .get // fails if assessment asset doesn't exist or can't json-decode

      attemptEntity.toAttempt(assessment, user)

  def throwNotAssessment(attemptId: Long, edgePath: EdgePath): Nothing =
    throw new RuntimeException(
      s"Failed to fetch attempt $attemptId because path $edgePath is not a ${classOf[SubmissionAssessmentAsset[?]].getName}"
    )

  // I think it would be real swell to just duplicate the shit out of two types of assessment
  // such that we have to duplicate the shit out of all the supporting code.
  override def getParticipationData(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment]
  ): Seq[AssessmentParticipationData] =
    val participation: Map[EdgePath, AssessmentParticipation] =
      submissionAttemptDao.getParticipationData(course.id, assessments.map(_.contentId.edgePath))
    // Plausibly one single query could produce all the data needed by all the grading policies
    // and we could then do one query followed by some grouping and mapping on the result set;
    // however, I hypostulate that most courses have not all policies, maybe even just one, and this
    // is easier and more maintainable and, should the hypostulate prove truthy, more efficient.
    val awaiting: Map[EdgePath, Int]                          = assessments
      .groupBy(_.gradingPolicy)
      .toList
      .map { case (policy, quizzes) =>
        submissionAttemptDao.attemptsAwaitingGrade(course.id, quizzes.map(_.contentId.edgePath), policy)
      }
      .suml
    assessments map { assessment =>
      val stats = participation.getOrElse(assessment.contentId.edgePath, AssessmentParticipation.zero)
      AssessmentParticipationData(
        identifier = assessment.contentId,
        validAttempts = stats.validAttempts,
        awaitingInstructorInput = awaiting.getOrElse(assessment.contentId.edgePath, 0),
        participantCount = stats.participantCount,
        latestUpdate = stats.latestUpdateDate
      )
    }
  end getParticipationData

  override def getLearnerAttemptOverviews(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment],
    userId: UserId
  ): Seq[LearnerAttemptOverview] =
    val counts: Seq[UserAttemptCounts] =
      submissionAttemptDao.aggregateUserAttempts(course.id, assessments.map(_.edgePath), userId.value)
    LearnerAttemptOverview.of(counts, assessments, userId.value)

  override def getInstructorAttemptsOverviews(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment]
  ): Seq[InstructorAttemptsOverview] =
    val counts: Seq[UserAttemptCounts] =
      submissionAttemptDao.aggregateUserAttempts(course.id, assessments.map(_.edgePath))
    InstructorAttemptsOverview.of(counts, assessments)

  override def invalidateAttempts(
    course: CourseSection,
    edgePaths: Seq[EdgePath],
    subject: UserId,
  ): Unit =
    val attempts = submissionAttemptDao.getUserAttempts(course.id, edgePaths, subject.id, forUpdate = true)
    attempts.map(entity =>
      entity.valid = false
      submissionAttemptDao.write(entity)
    )
  end invalidateAttempts

  override def transferAttempts(
    course: CourseSection,
    edgePaths: Seq[EdgePath],
    subject: UserId,
    destinationContextId: ContextId
  ): Unit =
    val attempts = submissionAttemptDao.getUserAttempts(course.id, edgePaths, subject.id, forUpdate = true)
    attempts.map(entity =>
      entity.contextId = destinationContextId.id
      submissionAttemptDao.write(entity)
    )
  end transferAttempts

  private def performAsDriver(
    attempt: SubmissionAttempt,
    driver: UserId,
    action: SubmissionAttemptAction
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    val selfDriven              = attempt.user.id.equals(driver.id)
    val instructorLike: Boolean = courseAccessService.hasInstructorAccess(attempt.contentId.contextId)
    validateDriver(attempt.assessment, selfDriven, instructorLike).widenl
      .flatMap(_ => perform(attempt, action))

  private def perform(
    attempt: SubmissionAttempt,
    action: SubmissionAttemptAction
  ): SubmissionAttemptFailure \/ SubmissionAttempt =
    for updatedAttempt <- action.exec(params(attempt))
    yield
      submissionAttemptDao.write(updatedAttempt.toEntity)
      dispatchEvents(params(updatedAttempt), action.events(params(updatedAttempt)))

      coursewareAnalyticsService.emitAttemptPutEvent(updatedAttempt, manualScore = true)

      updatedAttempt

  private def dispatchEvents(params: SubmissionAttemptActionParameters, events: Seq[SubmissionAttemptEvent]): Unit =

    val andSubmitted = events.exists(_.isInstanceOf[AttemptSubmittedEvent])
    val andFinalized = events.exists(_.isInstanceOf[AttemptFinalizedEvent])

    events.view
      .map({
        case AttemptSubmittedEvent(_) => AttemptSubmittedEvent(andFinalized)
        case AttemptFinalizedEvent(_) => AttemptFinalizedEvent(andSubmitted)
        case e                        => e
      })
      .foreach(event => submissionAssessmentEventDispatchService.dispatchEvent(params, event))
  end dispatchEvents

  /** Validates whether the user can perform an action. This checks assessment settings versus the given information
    * about the user.
    *
    * @param assessment
    *   the assessment the action is being taken against
    * @param selfDriven
    *   whether the user is the subject of the attempt
    * @param instructorLike
    *   whether the user is privilaged in the context of the assessment
    * @return
    *   whether the user is allowed to perform said action
    */
  private def validateDriver(
    assessment: SubmissionAssessment,
    selfDriven: Boolean,
    instructorLike: Boolean
  ): IllegalDriver \/ Unit =
    val driver = assessment.settings.driver
    if driver == Observation && !instructorLike then IllegalDriver(driver).left
    else if driver == SubjectDriven && !selfDriven then IllegalDriver(driver).left
    else ().right

  @inline
  private def params(attempt: SubmissionAttempt): SubmissionAttemptActionParameters =
    SubmissionAttemptActionParameters(attempt, attempt.assessment, ts.instant)
end SubmissionAttemptServiceImpl

object SubmissionAttemptServiceImpl:
  val logger: Logger = org.log4s.getLogger(classOf[SubmissionAttemptServiceImpl])
