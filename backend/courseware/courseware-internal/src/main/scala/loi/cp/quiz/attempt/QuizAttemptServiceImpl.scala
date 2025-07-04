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

package loi.cp.quiz.attempt

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.quiz.{QuizAttemptEntity, QuizAttemptFolderEntity}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.asset.assessment.model.AssessmentType
import loi.asset.lesson.model.Lesson
import loi.authoring.asset.Asset
import loi.authoring.edge.Group
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.appevent.AppEventService
import loi.cp.assessment.InstructorOverviews.InstructorAttemptsOverview
import loi.cp.assessment.LearnerOverviews.LearnerAttemptOverview
import loi.cp.assessment.Score.Epsilon
import loi.cp.assessment.*
import loi.cp.assessment.attempt.*
import loi.cp.assessment.settings.Limited
import loi.cp.content.ContentTree
import loi.cp.content.gate.ContentGateOverrideService
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.lwgrade.{Grade, GradeService, GradeStructure}
import loi.cp.mastery.MasteryService
import loi.cp.notification.NotificationService
import loi.cp.progress.{LightweightProgressService, ProgressChange}
import loi.cp.quiz.*
import loi.cp.quiz.attempt.QuizAttemptService.{AttachmentRequest, AutoSubmitGracePeriod, SelectionRequest}
import loi.cp.quiz.attempt.actions.*
import loi.cp.quiz.attempt.auto.AutoSubmitAttemptEvent
import loi.cp.quiz.attempt.event.*
import loi.cp.quiz.attempt.notification.AttemptInvalidationNotification
import loi.cp.quiz.persistence.{QuizAttemptDao, QuizAttemptFolderDao}
import loi.cp.quiz.question.*
import loi.cp.reference.*
import loi.cp.user.UserService
import org.log4s.Logger
import scalaz.Scalaz.ToOptionOpsFromOption
import scalaz.std.anyVal.*
import scalaz.std.list.*
import scalaz.std.map.*
import scalaz.syntax.traverse.*
import scalaz.{-\/, \/, \/-}
import scaloi.data.ListTree
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*
import scaloi.syntax.collection.*
import scaloi.syntax.date.*
import scaloi.syntax.option.*

import java.time.Instant
import java.util.{Date, UUID}
import scala.collection.mutable
import scala.concurrent.duration.*

/** The default implementation of {{QuizAttemptService}}.
  */
@Service
class QuizAttemptServiceImpl(
  coursewareAnalyticsService: CoursewareAnalyticsService,
  gradeService: GradeService,
  notificationService: NotificationService,
  progressService: LightweightProgressService,
  quizAttemptDao: QuizAttemptDao,
  quizAttemptFolderDao: QuizAttemptFolderDao,
  quizAttemptLoadService: QuizAttemptLoadService,
  quizEventDispatchService: QuizEventDispatchService,
  questionService: QuestionService,
  userService: UserService,
  masteryService: MasteryService,
  overrideService: ContentGateOverrideService,
  appEventService: AppEventService,
  domainDto: => DomainDTO,
  ts: TimeSource,
) extends QuizAttemptService:

  import QuizAttemptOps.*
  import QuizAttemptServiceImpl.logger

  override def getAttempts(course: CourseSection, quizzes: Seq[Quiz]): Seq[QuizAttempt] =
    val edgePaths                        = quizzes.validateContextOrThrow(course)
    val entities: Seq[QuizAttemptEntity] = quizAttemptDao.getAttempts(course.id, edgePaths)

    val quizByPath = quizzes.groupUniqBy(_.edgePath)
    val users      = userService.getUsers(entities.map(_.userId))

    for
      entity <- entities
      user   <- users.get(entity.userId)
    yield
      val quiz = quizByPath(EdgePath.parse(entity.edgePath))
      entity.toAttempt(quiz, user)
  end getAttempts

  override def getUserAttempts(course: CourseSection, quizzes: Seq[Quiz], user: UserDTO): Seq[QuizAttempt] =
    quizAttemptLoadService.getUserAttempts(course, quizzes, user)

  override def countValidAttempts(course: CourseSection, quizzes: Seq[Quiz]): Int =
    quizAttemptDao.countValidAttempts(course.id, quizzes.map(_.edgePath))

  override def countValidAttempts(course: CourseSection, quiz: Quiz, userId: UserId): Int =
    quizAttemptDao.countValidAttempts(course.id, quiz.edgePath, userId.value)

  override def createAttempt(
    quiz: Quiz,
    questions: Seq[Question],
    user: UserDTO
  ): AttemptLimitExceeded \/ QuizAttempt =
    for _ <- checkAttemptCount(quiz, user)
    yield
      val folder: QuizAttemptFolderEntity = quizAttemptFolderDao.getOrCreateAttemptFolder(user)
      val entity                          = initAttempt(quiz, questions, user, folder)
      val attempt: QuizAttempt            = entity.toAttempt(quiz, user)

      AssessmentLogMeta.attemptMetadata(attempt)
      logger.info("New attempt created")

      emitAttemptPutEvent(attempt, questions)

      attempt.maxMinutes foreach { minutes =>
        appEventService.scheduleEvent(
          Date.from(attempt.createTime) + minutes.minutes + AutoSubmitGracePeriod,
          user, // There is nowhere convenient to put this
          null,
          AutoSubmitAttemptEvent(quiz.section.id, attempt.id.value)
        )
      }

      attempt

  /** Validates that the given user has less valid attempts than the maximum given for the configuration for the given
    * quiz. Returns [[AttemptLimitExceeded]] if opening a new attempt would put the user over the attempt limit.
    *
    * @param quiz
    *   the quiz to check against
    * @param user
    *   the user in question
    * @return
    *   a [[AttemptLimitExceeded]] if exceeding the limit or nothing
    */
  private def checkAttemptCount(
    quiz: Quiz,
    user: UserDTO
  ): AttemptLimitExceeded \/ Unit =

    val validAttemptCount: Int =
      quizAttemptDao
        .getUserAttempts(quiz.contextId.value, Seq(quiz.edgePath), user.id)
        .count(_.valid)

    (validAttemptCount, quiz.settings.maxAttempts) match
      case (attemptCount, Limited(maxAttempts)) if attemptCount >= maxAttempts =>
        -\/(AttemptLimitExceeded(maxAttempts))
      case _                                                                   =>
        \/-(())
  end checkAttemptCount

  /** Initializes the persistence entity for an attempt, writing it out to the database.
    *
    * @param quiz
    *   the quiz the attempt is against
    * @param questions
    *   the questions for the quiz
    * @param user
    *   the user taking the attempt
    */
  private def initAttempt(
    quiz: Quiz,
    questions: Seq[Question],
    user: UserDTO,
    attemptFolder: QuizAttemptFolderEntity
  ): QuizAttemptEntity =
    val questionUsages: Seq[QuizAttemptQuestionUsage] = generateUsages(questions)
    val emptyResponses: Seq[QuizQuestionResponse]     = questionUsages.map(_ => QuizQuestionResponse.empty)

    val accommodation = overrideService.loadAccommodations(quiz.section, user).toOption.flatMap(_.get(quiz.edgePath))
    val maxMinutes    = accommodation.orElse(quiz.settings.maxMinutes).filterNZ // 0 means no limit

    quizAttemptDao.newAttempt(
      quiz,
      user,
      Date.from(ts.instant),
      maxMinutes,
      questionUsages,
      emptyResponses,
      attemptFolder,
      domainDto
    )
  end initAttempt

  /** Generates question usages which determine the display order of the distractors and is a persistable reference to
    * the question.
    *
    * @param questions
    *   the questions to generate usages for
    * @return
    *   the usages for the questions
    */
  def generateUsages(questions: Seq[Question]): Seq[QuizAttemptQuestionUsage] =
    val distractorOrderByQuestion: Map[Question, Option[DistractorOrder]] =
      questions
        .map({
          case rq: RandomizableQuestion if rq.allowDistractorRandomization => rq -> Some(rq.generateDistractorOrder())
          case q: Question                                                 => q  -> None
        })
        .toMap

    questions.map(question =>
      val ref: VersionedAssetReference             = question.contentReference
      val distractorOrder: Option[DistractorOrder] = distractorOrderByQuestion(question)
      QuizAttemptQuestionUsage(ref, distractorOrder)
    )
  end generateUsages

  override def submitAttempt(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt,
    autoSubmit: Boolean = false,
  ): QuizAttemptFailure \/ QuizAttempt =
    for newAttempt <- perform(attempt, SubmitAttempt(autoSubmit) `andThen` ConditionallyScoreAttempt())
    yield
      processMasteryFromAttempt(ws, section, newAttempt)
      unscheduleAutoSubmit(attempt)
      newAttempt

  override def testOut(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt
  ): QuizAttemptFailure \/ List[EdgePath] =
    for
      // diagnostics have one attempt and no policy so just take the score
      score <- attempt.score \/> NotYetScored(attempt)
    yield
      val percent   = score.asPercentage
      val quiz      = attempt.assessment
      val learner   = attempt.user
      val testOuts  = quiz.testsOut map { case (edgePath, threshold) =>
        edgePath -> (percent + Epsilon >= threshold / 100)
      }
      val testedOut = testOuts.filter(_._2).keys.toList

      logger.info(s"Testing out of ${testedOut.length} targets")

      val structure     = GradeStructure(section.contents)
      val gradebook     = gradeService.getGradebook(section, learner)
      val progress      = mutable.ListBuffer.empty[ProgressChange]
      val priorProgress = progressService.loadProgress(section, learner, gradebook)

      // There must be a treeomorphism for this but it is beyond me
      def loop(tree: ContentTree, inheritedPass: Boolean = false): Unit = tree match
        case ListTree.Node(content, subForest) =>
          val pass = testOuts.getOrElse(content.edgePath, inheritedPass)
          if pass then
            for
              column      <- structure.findColumnForEdgePath(content.edgePath)
              currentGrade = gradebook.get(content.edgePath).flatMap(Grade.fraction)
              if column.isForCredit && currentGrade.forall(_ < percent)
            do
              logger.info(s"Tested out of ${content.edgePath}")
              gradeService.setGradePercent(
                learner,
                section,
                content,
                structure,
                column,
                percent,
                ts.instant,
              )
            end for
            if !content.isContainer && !priorProgress.isComplete(content.edgePath) then
              progress += ProgressChange.testOut(content.edgePath)
          end if
          subForest.foreach(loop(_, pass))
      loop(section.contents.tree)

      logger.info(s"Marking ${progress.size} with progress")

      // I must reload the gradebook here because the grades will have changed
      progressService
        .updateProgress(section, learner, gradeService.getGradebook(section, learner), progress.toList)
        .valueOr(e => throw new RuntimeException(e.msg))

      testedOut

  override def invalidateAttempt(attempt: QuizAttempt): QuizAttemptFailure \/ QuizAttempt =
    val questions =
      questionService.getQuestions(attempt.assessment.section, attempt.questions.map(_.questionPointer)).toList
    AssessmentLogMeta.attemptMetadata(attempt)

    for _ <- attempt.valid.elseLeft(AlreadyInvalidFailure(attempt))
    yield
      val updatedAttempt = attempt.copy(
        valid = false,
        updateTime = ts.instant
      )
      quizAttemptDao.write(updatedAttempt.toEntity)

      val quiz                  = updatedAttempt.assessment
      val section               = updatedAttempt.assessment.section
      val learner               = updatedAttempt.user
      val userAttempts          = getUserAttempts(section, Seq(quiz), learner) // ! query !
      val numValidAttempts      = userAttempts.count(_.valid)
      val manualScore           = questions.exists(_.isInstanceOf[ManualGrading])
      val gradeStructure        = GradeStructure(section.contents)
      val gradeColumn           = gradeStructure.findColumnForEdgePath(quiz.edgePath)
      val gradingPolicy         = quiz.settings.gradingPolicy
      val attemptsAwaitingGrade = gradingPolicy.attemptsAwaitingGrade(userAttempts)
      val grade                 = gradingPolicy.getGrade(userAttempts)

      coursewareAnalyticsService.emitAttemptPutEvent(updatedAttempt, manualScore)

      gradeColumn.foreach(gradeColumn =>
        grade match
          case None               =>
            if attemptsAwaitingGrade.nonEmpty then
              // attempt.updateTime may not be the actual attempt that determines the grade
              gradeService.setGradePending(
                learner,
                section,
                quiz.courseContent,
                gradeStructure,
                gradeColumn,
                updatedAttempt.updateTime
              )
            else gradeService.unsetGrade(learner, section, quiz.courseContent)
          case Some(gradePercent) =>
            gradeService.setGradePercent(
              learner,
              section,
              quiz.courseContent,
              gradeStructure,
              gradeColumn,
              gradePercent,
              attempt.updateTime,
            )
      )

      if gradeColumn.isEmpty then
        // the grade column will not exist when the assessment is a course child
        // such a structure cannot be authored today, but was in the past and remains in use for diagnostic.1s
        // there could be other scenarios but no one has mentioned them to us in the past 3ish years
        logger.info(s"No gradebook column")

      val progressChanges = if numValidAttempts == 0 then List(ProgressChange.unvisit(quiz.edgePath)) else Nil
      progressService
        .updateProgress(section, learner, gradeService.getGradebook(section, learner), progressChanges)
        .valueOr(e => throw new RuntimeException(e.msg))

      unscheduleAutoSubmit(attempt)

      notificationService.nοtify[AttemptInvalidationNotification](
        learner.id,
        AttemptInvalidationNotification.Init(
          quiz.title,
          quiz.edgePath,
          updatedAttempt.id,
          updatedAttempt.contentId.contextId,
          learner.id,
          ts.instant
        )
      )

      updatedAttempt
    end for
  end invalidateAttempt

  override def respond(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt,
    selectionRequests: Seq[SelectionRequest],
    attachmentRequests: Seq[AttachmentRequest],
    submitResponse: Boolean = false,
    autoSubmit: Boolean = false,
  ): QuizAttemptFailure \/ QuizAttempt =
    def responseAction(resp: SelectionRequest): QuizAttemptAction =
      if resp.submitResponse then
        (SelectResponse(resp.questionIndex, resp.selection)
          `andThen` SubmitResponse(resp.questionIndex)
          `andThen` ConditionallyScoreResponse(resp.questionIndex))
      else SelectResponse(resp.questionIndex, resp.selection)

    val responseActions: QuizAttemptAction =
      AggregateAttemptAction(
        attachmentRequests.map(request => SetResponseAttachments(request.questionIndex, request.attachments)) ++
          selectionRequests.map(responseAction)
      )

    val submit = if submitResponse then SubmitAttempt(autoSubmit) else ConditionallySubmitAttempt(autoSubmit)

    for newAttempt <-
        perform(attempt, responseActions `andThen` submit `andThen` ConditionallyScoreAttempt())
    yield
      if newAttempt.state != AttemptState.Open then unscheduleAutoSubmit(attempt)
      processMasteryFromAttempt(ws, section, newAttempt)
      newAttempt
  end respond

  override def draftResponseScore(
    attempt: QuizAttempt,
    questionIndex: Int,
    score: Option[ResponseScore],
    scorer: Option[Long]
  ): QuizAttemptFailure \/ QuizAttempt =
    perform(attempt, DraftResponseScore(questionIndex, score, scorer))

  override def submitResponseScore(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt,
    questionIndex: Int,
    score: ResponseScore,
    scorer: Option[Long]
  ): QuizAttemptFailure \/ QuizAttempt =
    for newAttempt <-
        perform(attempt, SubmitResponseScore(questionIndex, score, scorer) `andThen` ConditionallyScoreAttempt())
    yield
      processMasteryFromAttempt(ws, section, newAttempt)
      newAttempt

  override def setResponseFeedback(
    attempt: QuizAttempt,
    questionIndex: Int,
    feedback: Seq[Feedback],
    release: Boolean
  ): QuizAttemptFailure \/ QuizAttempt =

    val questions = questionService.getQuestions(attempt.assessment.section, attempt.questions.map(_.questionPointer))

    for
      _ <- attempt.valid.elseLeft(InvalidatedAttemptScoringFailure).widenl
      _ <- AttemptResponseUtils.validateResponseIndex(questions, questionIndex).widenl
    yield
      val response         = attempt.responses(questionIndex)
      val updatedResponses = attempt.responses.updated(
        questionIndex,
        response.copy(
          instructorFeedback = feedback,
          instructorFeedbackReleased = release
        )
      )

      val updatedAttempt = attempt.copy(
        responses = updatedResponses,
        updateTime = ts.instant
      )
      quizAttemptDao.write(updatedAttempt.toEntity)
      emitAttemptPutEvent(updatedAttempt, questions)
      updatedAttempt
    end for
  end setResponseFeedback

  private def lastScorerId(a: QuizAttempt): Option[Long] =
    a.responses
      .filter(_.state == QuestionResponseState.ResponseScoreReleased)
      .flatMap(r => for (scorerId <- r.scorer; scoreTime <- r.scoreTime) yield (scorerId, scoreTime))
      .sorted(using (x: (Long, Instant), y: (Long, Instant)) => y._2.compareTo(x._2))
      .headOption
      .map(_._1)

  private def loadUser(userId: Long): UserDTO =
    userService.getUser(userId).getOrElse(throw new IllegalStateException(s"No such user $userId"))

  override def fetch(
    context: CourseSection,
    attemptId: Long,
    policies: List[CourseAssessmentPolicy]
  ): Option[QuizAttempt] =
    fetch(context, attemptId, lock = false, policies)

  override def fetchForUpdate(
    context: CourseSection,
    attemptId: Long,
    policies: List[CourseAssessmentPolicy]
  ): Option[QuizAttempt] =
    fetch(context, attemptId, lock = true, policies)

  private def fetch(
    context: CourseSection,
    attemptId: Long,
    lock: Boolean,
    policies: List[CourseAssessmentPolicy]
  ): Option[QuizAttempt] =
    for
      attemptEntity <- quizAttemptDao.load(AttemptId(attemptId), lock)
      content       <- context.contents.get(EdgePath.parse(attemptEntity.edgePath))
      if attemptEntity.contextId == context.id
      user          <- userService.getUser(attemptEntity.userId)
    yield
      val quiz = Quiz
        .fromContent(content, context, policies) // TODO: I think this code path is not used
        .getOrElse(throwNotQuizlike(attemptId, content.edgePath))

      attemptEntity.toAttempt(quiz, user)

  def throwNotQuizlike(attemptId: Long, edgePath: EdgePath): Nothing =
    throw new RuntimeException(
      s"Failed to fetch attempt $attemptId because path $edgePath is not a ${classOf[QuizAsset[?]].getName}"
    )

  private def questionsByAttempt(attempts: Seq[QuizAttempt]): Map[QuizAttempt, Seq[Question]] =
    if attempts.isEmpty then return Map.empty // get thee behind me
    val section                                                = attempts.head.assessment.section
    assert(attempts.forall(_.assessment.section.id == section.id))
    val allQuestionRefs: Seq[VersionedAssetReference]          = attempts.flatMap(_.questions.map(_.questionPointer))
    val questions: Seq[Question]                               = questionService.getQuestions(section, allQuestionRefs)
    val questionsByRef: Map[VersionedAssetReference, Question] = questions.groupUniqBy(_.contentReference)

    attempts
      .map(attempt =>
        val attemptRefs: Seq[VersionedAssetReference] = attempt.questions.map(_.questionPointer)
        val attemptQuestions: Seq[Question]           = attemptRefs.map(questionsByRef)
        attempt -> attemptQuestions
      )
      .toMap
  end questionsByAttempt

  override def getParticipationData(course: CourseSection, quizzes: Seq[Quiz]): Seq[AssessmentParticipationData] =
    val participation: Map[EdgePath, AssessmentParticipation] =
      quizAttemptDao.getParticipationData(course.id, quizzes.map(_.contentId.edgePath))
    // Plausibly one single query could produce all the data needed by all the grading policies
    // and we could then do one query followed by some grouping and mapping on the result set;
    // however, I hypostulate that most courses have not all policies, maybe even just one, and this
    // is easier and more maintainable and, should the hypostulate prove truthy, more efficient.
    val awaiting: Map[EdgePath, Int]                          = quizzes
      .groupBy(_.gradingPolicy)
      .toList
      .map { case (policy, quizzes) =>
        quizAttemptDao.attemptsAwaitingGrade(course.id, quizzes.map(_.contentId.edgePath), policy)
      }
      .suml
    quizzes map { quiz =>
      val stats = participation.getOrElse(quiz.contentId.edgePath, AssessmentParticipation.zero)
      AssessmentParticipationData(
        identifier = quiz.contentId,
        validAttempts = stats.validAttempts,
        awaitingInstructorInput = awaiting.getOrElse(quiz.contentId.edgePath, 0),
        participantCount = stats.participantCount,
        latestUpdate = stats.latestUpdateDate
      )
    }
  end getParticipationData

  override def getLearnerAttemptOverviews(
    course: CourseSection,
    quizzes: Seq[Quiz],
    userId: UserId
  ): Seq[LearnerAttemptOverview] =
    val counts: Seq[UserAttemptCounts] =
      quizAttemptDao.aggregateUserAttempts(course.id, quizzes.map(_.edgePath), userId.value)
    LearnerAttemptOverview.of(counts, quizzes, userId.value)

  override def getInstructorAttemptsOverviews(
    course: CourseSection,
    quizzes: Seq[Quiz]
  ): Seq[InstructorAttemptsOverview] =
    val counts: Seq[UserAttemptCounts] = quizAttemptDao.aggregateUserAttempts(course.id, quizzes.map(_.edgePath))
    InstructorAttemptsOverview.of(counts, quizzes)

  override def invalidateAttempts(
    course: CourseSection,
    edgePaths: Seq[EdgePath],
    user: UserId,
  ): Unit =
    quizAttemptDao
      .getUserAttempts(course.id, edgePaths, user.value, forUpdate = true)
      .foreach(entity =>
        entity.valid = false
        quizAttemptDao.write(entity)
      )

  override def transferAttempts(
    course: CourseSection,
    edgePaths: Seq[EdgePath],
    user: UserId,
    destinationContextId: ContextId
  ): Unit =
    quizAttemptDao
      .getUserAttempts(course.id, edgePaths, user.value, forUpdate = true)
      .foreach(entity =>
        entity.contextId = destinationContextId.id
        quizAttemptDao.write(entity)
      )

  private def perform(attempt: QuizAttempt, action: QuizAttemptAction): QuizAttemptFailure \/ QuizAttempt =
    val questions: Seq[Question] =
      questionService.getQuestions(attempt.assessment.section, attempt.questions.map(_.questionPointer))
    for updatedAttempt <- action.exec(QuizActionParameters(attempt, attempt.assessment, questions, ts.instant))
    yield
      quizAttemptDao.write(updatedAttempt.toEntity)
      val parameters = QuizActionParameters(updatedAttempt, attempt.assessment, questions, ts.instant)
      dispatchEvents(parameters, action.events(parameters))
      emitAttemptPutEvent(updatedAttempt, questions)
      updatedAttempt
  end perform

  private def dispatchEvents(params: QuizActionParameters, events: Seq[QuizAttemptEvent]): Unit =
    AssessmentLogMeta.attemptMetadata(params.attempt)

    val andSubmitted = events.exists(_.isInstanceOf[AttemptSubmittedEvent])
    val andFinalized = events.exists(_.isInstanceOf[AttemptFinalizedEvent])

    events.view
      .map({
        case AttemptSubmittedEvent(_) => AttemptSubmittedEvent(andFinalized)
        case AttemptFinalizedEvent(_) => AttemptFinalizedEvent(andSubmitted)
        case e                        => e
      })
      .foreach(event => quizEventDispatchService.dispatchEvent(params, event))
  end dispatchEvents

  private def emitAttemptPutEvent(attempt: QuizAttempt, questions: Seq[Question]): Unit =
    val manualScore = questions.exists(_.isInstanceOf[ManualGrading])
    coursewareAnalyticsService.emitAttemptPutEvent(attempt, manualScore)

  private def processMasteryFromAttempt(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt
  ): Unit =
    if attempt.state == AttemptState.Finalized &&
      attempt.assessment.settings.assessmentType == AssessmentType.Summative
    then masteryService.updateUserMasteryForQuizAttempt(ws, section, attempt)
    else if attempt.state == AttemptState.Finalized &&
      attempt.assessment.isDiagnostic
    then
      logger.info(s"Processing diagnostic attempt ${attempt.id}")
      val proficiency = masteryService.computeMasteryForQuizAttempt(ws, attempt)
      grantProgress(ws, attempt.user, section, proficiency)

  private def grantProgress(
    ws: AttachedReadWorkspace,
    user: UserDTO,
    section: CourseSection,
    competencyProficiency: Set[UUID]
  ): Unit =
    // Progress is given to the immediate children of Lessons that Teach mastered competencies
    def isMastered(asset: Asset[?]): Boolean =
      val teaches = ws.outEdgeAttrs(asset.info.name, Group.Teaches)
      teaches.nonEmpty && teaches.forall(edge => competencyProficiency.contains(edge.tgtName))

    val masteredChildren = section.contents.tree
      .findSubtrees(n => n.asset.is[Lesson] && isMastered(n.asset))
      .flatMap(_.flatten)
      .filterNot(_.isContainer)

    // Progress from mastery is not granted to children that are For Credit (we still want students to get grades here)
    val masteredNonCreditChildren = masteredChildren.filterNot(_.gradingPolicy.exists(_.isForCredit))

    logger.info(s"Marking ${masteredNonCreditChildren.size} with progress")

    val progressChanges: List[ProgressChange] = masteredNonCreditChildren
      .map(c => ProgressChange.testOut(c.edgePath))

    progressService
      .updateProgress(section, user, gradeService.getGradebook(section, user), progressChanges)
      .valueOr(e => throw new RuntimeException(e.msg))
  end grantProgress

  private def unscheduleAutoSubmit(attempt: QuizAttempt): Unit = {
    // Unfortunately attempt isn't in the tree so I cannot specify it as the target so I
    // can't find and delete the app-event.
    // if (attempt.maxMinutes.isDefined)
    //   appEventService.deleteEvents(attempt.user, attempt, classOf[AutoSubmitAttemptEvent])
  }
end QuizAttemptServiceImpl

object QuizAttemptServiceImpl:
  val logger: Logger = org.log4s.getLogger
