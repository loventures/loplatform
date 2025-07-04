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

package loi.cp.analytics.redshift

import cats.instances.option.*
import cats.kernel.Monoid
import cats.syntax.apply.*
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.Analytic
import loi.cp.analytics.entity.UserData
import loi.cp.analytics.event.*
import loi.cp.assessment.attempt.AttemptState
import loi.cp.email.EmailAddress
import mouse.boolean.*

import java.sql.Timestamp
import java.time.Duration
import scala.math.BigDecimal.RoundingMode

object RedshiftTransform:

  def transformEvent(e: Analytic): RedshiftPayload =
    e.eventData match
      case e: SectionEntryEvent1               => transformForSectionEntryEvent1(e)
      case e: SectionCreateEvent2              => transformForSectionCreateEvent2(e)
      case e: SectionUpdateEvent2              => transformForSectionUpdateEvent2(e)
      case e: SectionDeleteEvent1              => transformForSectionDeleteEvent1(e)
      case e: PublishContentEvent1             => transformForPublishContentEvent1(e)
      case e: GradePutEvent1                   => transformForGradePutEvent1(e)
      case e: GradeUnsetEvent                  => transformForGradeUnsetEvent(e)
      case e: SessionEvent                     => transformForSessionEvent(e)
      case e: SurveySubmissionEvent1           => transformForSurveySubmissionEvent1(e)
      case e: SurveySubmissionEvent2           => transformForSurveySubmissionEvent2(e)
      case e: TimeSpentEvent2                  => transformForTimeSpentEvent2(e)
      case e: DiscussionPostPutEvent1          => transformForDiscussionPostPutEvent1(e)
      case e: EnrollmentCreateEvent2           => transformForEnrollmentCreateEvent2(e)
      case e: EnrollmentUpdateEvent2           => transformForEnrollmentUpdateEvent2(e)
      case e: EnrollmentDeleteEvent1           => transformForEnrollmentDeleteEvent1(e)
      case e: InstructorSnapshotDayCreatEvent1 => transformForInstructorSnapshotDayCreatEvent1(e)
      case e: AttemptPutEvent1                 => transformForAttemptPutEvent1(e)
      case e: PageNavEvent                     => transformForPageNavEvent(e)
      case e: ProgressPutEvent2                => transformForProgressPutEvent2(e)
      case e: TutorialViewEvent1               => transformForTutorialViewEvent1(e)
      case e: UserObfuscateEvent1              => transformForUserObfuscateEvent1(e)
      case e: QnaThreadPutEvent1               => transformForQnaThreadPutEvent1(e)
      case _                                   => Monoid[RedshiftPayload].empty

  private def transformForSectionEntryEvent1(e: SectionEntryEvent1): RedshiftPayload =
    RedshiftPayload(
      sectionEntries = List(
        RsSectionEntry(
          e.user.id,
          e.sectionId,
          new Timestamp(e.time.getTime),
          e.sessionId,
          Some(e.role),
          e.originSectionId
        )
      ),
      users = List(rsUser(e.user)),
    )

  private def rsSectionContent(sectionId: Long)(c: PublishContentEvent.Content1): RsSectionContent = RsSectionContent(
    sectionId,
    c.edgePath,
    c.assetId,
    c.parent.map(_.edgePath),
    c.parent.map(_.assetId),
    c.course.map(_.assetId),
    c.module.map(_.edgePath),
    c.module.map(_.assetId),
    c.lesson.map(_.edgePath),
    c.lesson.map(_.assetId),
    c.forCreditPointsPossible,
    c.forCreditItemCount,
    c.learningPathIndex,
  )

  private def transformForSectionCreateEvent2(e: SectionCreateEvent2): RedshiftPayload =

    val system = e.integration.flatMap(_.system)

    val contents = Option(e.contents).getOrElse(Nil)

    RedshiftPayload(
      sections = List(
        RsSection(
          e.section.id,
          e.section.externalId,
          Some(e.section.name),
          e.integration.map(_.uniqueId),
          system.map(_.id),
          system.map(_.systemId),
          system.map(_.name),
          Some(e.offeringId),
          Some(e.offeringGroupId),
          Some(e.offeringName),
          Some(e.disabled),
          e.startDate.map(Timestamp.from),
          e.endDate.map(Timestamp.from),
          e.groupId,
        )
      ),
      sectionContents = contents.map(rsSectionContent(e.section.id))
    )
  end transformForSectionCreateEvent2

  private def transformForSectionUpdateEvent2(e: SectionUpdateEvent2): RedshiftPayload =
    val system = e.integration.flatMap(_.system)

    RedshiftPayload(sections =
      List(
        RsSection(
          e.section.id,
          e.section.externalId,
          Some(e.section.name),
          e.integration.map(_.uniqueId),
          system.map(_.id),
          system.map(_.systemId),
          system.map(_.name),
          Some(e.offeringId),
          Some(e.offeringGroupId),
          Some(e.offeringName),
          Some(e.disabled),
          e.startDate.map(Timestamp.from),
          e.endDate.map(Timestamp.from),
          e.groupId,
        )
      )
    )
  end transformForSectionUpdateEvent2

  private def transformForSectionDeleteEvent1(e: SectionDeleteEvent1): RedshiftPayload =
    RedshiftPayload(sectionDeletes = Set(e.sectionId))

  private def transformForPublishContentEvent1(e: PublishContentEvent1): RedshiftPayload =

    val assets =
      for
        // Jackson and Scala do not get along
        asset <- Option(e.assets).getOrElse(Nil)
      yield RsAsset(
        asset.id,
        asset.name.toString,
        asset.typeId,
        asset.title,
        asset.keywords,
        asset.forCredit,
        asset.pointsPossible.map(BigDecimal.apply)
      )

    val sectionContents = for
      // Jackson and Scala do not get along
      sectionId <- Option(e.sectionIds).getOrElse(Nil)
      content   <- Option(e.contents).getOrElse(Nil)
    yield rsSectionContent(sectionId)(content)

    RedshiftPayload(
      assets = assets,
      sectionContents = sectionContents,
    )
  end transformForPublishContentEvent1

  private def transformForGradePutEvent1(e: GradePutEvent1): RedshiftPayload =

    val awarded  = BigDecimal(e.score.pointsAwarded)
    val possible = BigDecimal(e.score.pointsPossible)

    val grade = RsGrade(
      e.learner.id,
      e.section.id,
      e.edgePath,
      e.assetId,
      new Timestamp(e.time.getTime),
      e.forCredit,
      awarded,
      possible,
      if possible == 0 then BigDecimal(0) else ((awarded / possible) * 100).setScale(2, RoundingMode.HALF_UP)
    )

    val activityKeys =
      e.maintenance.contains(true).zeroOrValue(Set(ContentKey(grade.sectionId, grade.userId, grade.edgePath)))

    RedshiftPayload(
      grades = Map(grade.key -> grade),
      activityKeys = activityKeys
    )
  end transformForGradePutEvent1

  private def transformForGradeUnsetEvent(e: GradeUnsetEvent): RedshiftPayload =

    val activityKeys = e.maintenance
      .contains(true)
      .zeroOrValue(Set(ContentKey(e.course.section.id, e.learner.id, e.contentId.contentId)))

    RedshiftPayload(
      unsetGrades = Set(RsGrade.Key(e.learner.id, e.course.section.id, e.contentId.contentId)),
      activityKeys = activityKeys
    )
  end transformForGradeUnsetEvent

  private def transformForSessionEvent(e: SessionEvent): RedshiftPayload =

    val sessions =
      if e.actionType == EventActionType.START then
        e.sessionId
          .map(sessionId =>
            RsSession(
              sessionId,
              new Timestamp(e.time.getTime),
              e.user.id,
              e.user.externalId,
              Option(e.requestUrl).map(_.take(1024)),
              Option(e.ipAddress).map(_.take(256)),
              Option(e.referrer).map(_.take(1024)),
              Option(e.acceptLanguage).map(_.take(2048)),
              Option(e.userAgent).map(_.take(1024)),
              e.authMethod.map(_.toString),
            )
          )
          .toList
      else Nil

    RedshiftPayload(
      sessions = sessions,
      sessionEvents = List(
        RsSessionEvent(
          e.id.toString,
          new Timestamp(e.time.getTime),
          Option(e.source),
          e.sessionId,
          e.actionType.toString,
          e.user.id,
          e.user.externalId,
          Option(e.requestUrl).map(_.take(1024)),
          Option(e.ipAddress).map(_.take(256)),
          Option(e.referrer).map(_.take(1024)),
          Option(e.acceptLanguage).map(_.take(2048)),
          Option(e.userAgent).map(_.take(1024)),
          e.authMethod.map(_.toString),
          e.lastActive.map(d => new Timestamp(d.getTime)),
          e.becameActive.map(d => new Timestamp(d.getTime)),
        )
      )
    )
  end transformForSessionEvent

  private def transformForSurveySubmissionEvent2(e: SurveySubmissionEvent2) =

    // Jackson and Scala do not get along
    val responses = Option(e.responses).getOrElse(Nil)

    RedshiftPayload(
      surveyQuestionResponses = responses.map(r =>
        RsSurveyQuestionResponse(
          e.userId,
          e.sectionId,
          e.attemptId.toString,
          new Timestamp(e.time.getTime),
          e.contentAssetId,
          e.contentEdgePath,
          r.questionAssetId,
          r.response.take(4096),
          Some(e.surveyAssetId),
          Some(e.surveyEdgePath)
        )
      )
    )
  end transformForSurveySubmissionEvent2

  private def transformForSurveySubmissionEvent1(e: SurveySubmissionEvent1): RedshiftPayload =

    // Jackson and Scala do not get along
    val responses = Option(e.responses).getOrElse(Nil)

    RedshiftPayload(
      surveyQuestionResponses = responses.map(r =>
        RsSurveyQuestionResponse(
          e.userId,
          e.sectionId,
          e.attemptId.toString,
          new Timestamp(e.time.getTime),
          e.contentAssetId,
          e.contentEdgePath,
          r.questionAssetId,
          r.response.take(4096),
          None,
          None
        )
      )
    )
  end transformForSurveySubmissionEvent1

  private def transformForTimeSpentEvent2(e: TimeSpentEvent2): RedshiftPayload =

    val timeSpentDiscrete = (e.edgePath, e.assetId).mapN((edgePath, assetId) =>
      RsTimeSpentDiscrete(
        e.user.id,
        edgePath,
        e.context.id,
        new Timestamp(e.time.getTime),
        e.durationSpent,
        assetId,
        e.id.toString,
        e.originSectionId,
      )
    )

    val activityKeys = e.maintenance
      .contains(true)
      .zeroOrValue(timeSpentDiscrete.map(a => ContentKey(a.sectionId, a.userId, a.edgePath)).toSet)

    RedshiftPayload(
      users = List(rsUser(e.user)),
      timeSpentDiscrete = timeSpentDiscrete.toList,
      activityKeys = activityKeys
    )
  end transformForTimeSpentEvent2

  def rsUser(e: UserData): RsUser =
    val emailDomain = e.email.flatMap(EmailAddress.unapply).map(_._2)
    RsUser(
      e.id,
      e.externalId,
      e.email,
      e.userName,
      emailDomain,
      e.givenName,
      e.familyName,
      e.fullName,
      e.subtenantId,
      e.subtenantName,
      e.integration,
      e.uniqueId
    )
  end rsUser

  private def transformForEnrollmentCreateEvent2(e: EnrollmentCreateEvent2): RedshiftPayload =

    RedshiftPayload(
      enrollments = List(
        RsEnrollment(
          e.enrollmentId,
          e.user.id,
          e.sectionId,
          e.role.take(64),
          e.disabled,
          e.startTime.map(Timestamp.from),
          e.endTime.map(Timestamp.from),
          e.dataSource.map(_.take(255))
        )
      ),
      users = List(rsUser(e.user))
    )

  private def transformForEnrollmentUpdateEvent2(e: EnrollmentUpdateEvent2): RedshiftPayload =
    RedshiftPayload(
      enrollments = List(
        RsEnrollment(
          e.enrollmentId,
          e.user.id,
          e.sectionId,
          e.role.take(64),
          e.disabled,
          e.startTime.map(Timestamp.from),
          e.endTime.map(Timestamp.from),
          e.dataSource.map(_.take(255))
        )
      ),
      users = List(rsUser(e.user))
    )

  private def transformForEnrollmentDeleteEvent1(e: EnrollmentDeleteEvent1): RedshiftPayload =
    RedshiftPayload(enrollmentDeletes = Set(e.enrollmentId))

  private def transformForAttemptPutEvent1(e: AttemptPutEvent1): RedshiftPayload =
    val (timeToScoreHour, timeToScoreDay) = (for
      submitTime <- e.submitTime
      scoreTime  <- e.scoreTime
      duration    = Duration.between(submitTime, scoreTime) if !duration.isNegative
    yield
      // the integer division round-down effect is desired
      val timeToScoreHour = (duration.getSeconds / 3600).toInt
      val timeToScoreDay  = (duration.getSeconds / (3600 * 24)).toInt
      (timeToScoreHour, timeToScoreDay)
    ).unzip

    val scorePercentage = for
      awarded  <- e.scorePointsAwarded
      possible <- e.scorePointsPossible
    yield if possible == 0 then BigDecimal(0) else ((awarded / possible) * 100).setScale(2, RoundingMode.HALF_UP)

    val attempts = List(
      RsAttempt(
        e.attemptId,
        e.userId,
        e.sectionId,
        e.edgePath,
        e.assetId,
        e.state,
        e.valid,
        e.manualScore,
        Timestamp.from(e.createTime),
        e.submitTime.map(Timestamp.from),
        e.scoreTime.map(Timestamp.from),
        e.scorePointsAwarded,
        e.scorePointsPossible,
        scorePercentage,
        e.scorerUserId,
        timeToScoreHour,
        timeToScoreDay,
        Some(new Timestamp(e.time.getTime)),
        e.maxMinutes.map(_.toInt),
        e.autoSubmitted,
      )
    )

    val submittedAttempts = attempts.filterNot(_.state == AttemptState.Open.entryName)

    val activityKeys = e.maintenance
      .contains(true)
      .zeroOrValue(submittedAttempts.map(a => ContentKey(a.sectionId, a.userId, a.edgePath)).toSet)

    RedshiftPayload(
      attempts = attempts,
      submittedAttempts = submittedAttempts,
      activityKeys = activityKeys
    )
  end transformForAttemptPutEvent1

  private def transformForDiscussionPostPutEvent1(e: DiscussionPostPutEvent1): RedshiftPayload =

    val hourLag = e.instructorReplyTime
      .map(replyTime => Duration.between(e.createTime, replyTime))
      .filter(!_.isNegative)
      .map(duration => (duration.getSeconds / 3600).toInt)

    RedshiftPayload(
      discussionPosts = List(
        RsDiscussionPost(
          e.postId,
          e.userId,
          e.sectionId,
          e.edgePath,
          e.assetId,
          e.role,
          e.depth,
          Timestamp.from(e.createTime),
          e.instructorReplyUserId,
          e.instructorReplyTime.map(Timestamp.from),
          hourLag,
        )
      )
    )
  end transformForDiscussionPostPutEvent1

  private def transformForQnaThreadPutEvent1(e: QnaThreadPutEvent1): RedshiftPayload =
    val hourLag = e.instructorReplyTime
      .map(replyTime => Duration.between(e.createTime, replyTime))
      .filter(!_.isNegative)
      .map(duration => (duration.getSeconds / 3600).toInt)

    RedshiftPayload(
      qnaThreads = List(
        RsQnaThread(
          e.threadId,
          e.questionId,
          e.userId,
          e.sectionId,
          e.edgePath,
          // e.assetId,
          Timestamp.from(e.createTime),
          e.instructorReplyUserId,
          e.instructorReplyTime.map(Timestamp.from),
          hourLag,
          e.studentClosed,
          e.category,
          e.subcategory,
        )
      )
    )
  end transformForQnaThreadPutEvent1

  private def transformForPageNavEvent(e: PageNavEvent): RedshiftPayload =
    RedshiftPayload(
      pageNavEvents = List(
        RsPageNavEvent(
          e.id.toString,
          new Timestamp(e.time.getTime),
          Option(e.source).map(_.take(1024)),
          e.session,
          e.user.id,
          e.user.externalId,
          e.url.take(1024),
          e.title.map(_.take(1024)),
          e.course.map(_.section.id),
          e.course.flatMap(_.section.externalId),
          e.content.map(_.contentId),
          e.contentType.map(_.entryName),
          e.contentTitle,
          e.er,
        )
      )
    )

  private def transformForInstructorSnapshotDayCreatEvent1(e: InstructorSnapshotDayCreatEvent1): RedshiftPayload =
    RedshiftPayload(instructorSnapshotDays = List(e.snapshotDay))

  private def transformForProgressPutEvent2(e: ProgressPutEvent2): RedshiftPayload =

    val progressOverTimes = e.values.map(pv =>
      RsProgressOverTime(
        e.sectionId,
        e.userId,
        pv.edgePath,
        pv.assetId,
        new Timestamp(e.time.getTime),
        pv.completions,
        pv.total,
        pv.visited,
        pv.testedOut,
        Some(pv.skipped),
        pv.forCreditGrades,
        pv.forCreditGradesPossible,
        pv.percentage,
      )
    )

    val activityKeys = e.maintenance
      .contains(true)
      .zeroOrValue(e.values.view.map(pv => ContentKey(e.sectionId, e.userId, pv.edgePath)).toSet)

    RedshiftPayload(
      progresses = progressOverTimes.map(_.rsProgress),
      progressOverTimes = progressOverTimes,
      activityKeys = activityKeys
    )
  end transformForProgressPutEvent2

  private def transformForTutorialViewEvent1(e: TutorialViewEvent1): RedshiftPayload =
    val tutorialView = RsTutorialView(
      userId = e.userId,
      time = new Timestamp(e.time.getTime),
      tutorialName = e.tutorialName,
      autoPlay = e.autoPlay,
      step = e.step,
      complete = e.complete,
    )

    RedshiftPayload(
      tutorialViews = List(tutorialView)
    )
  end transformForTutorialViewEvent1

  private def transformForUserObfuscateEvent1(e: UserObfuscateEvent1): RedshiftPayload =
    RedshiftPayload(
      userObfuscations = e.userId :: Nil
    )
end RedshiftTransform
