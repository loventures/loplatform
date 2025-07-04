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

import cats.effect.unsafe.implicits.global
import cats.instances.list.*
import cats.instances.option.*
import cats.syntax.foldable.*
import cats.syntax.list.*
import cats.syntax.traverse.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import com.learningobjects.cpxp.service.domain.DomainDTO
import doobie.implicits.*
import loi.cp.analytics.DeliveryResult.permanentFailure
import loi.cp.analytics.bus.{AnalyticBus, AnalyticBusConfiguration, AnalyticBusService, AnalyticBusState}
import loi.cp.analytics.{Analytic, AnalyticsSender, DeliveryResult}
import loi.db.Redshift
import loi.doobie.log.*
import org.apache.commons.lang3.StringUtils
import scalaz.syntax.std.`try`.*
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success, Try}

// see readme.md for overview
@Service
class RedshiftEventSender(
  domainDto: => DomainDTO,
  rsActivityDao: RsActivityDao,
  rsAssetDao: RsAssetDao,
  rsAttemptDao: RsAttemptDao,
  rsEnrollmentDao: RsEnrollmentDao,
  rsDiscussionPostDao: RsDiscussionPostDao,
  rsGradeDao: RsGradeDao,
  rsPageNavEventDao: RsPageNavEventDao,
  rsProgressDao: RsProgressDao,
  rsProgressOverTimeDao: RsProgressOverTimeDao,
  rsQnaQuestionDao: RsQnaThreadDao,
  rsInstructorSnapshotDailyDao: RsInstructorSnapshotDayDao,
  rsSectionDao: RsSectionDao,
  rsSectionEntryDao: RsSectionEntryDao,
  rsSectionContentDao: RsSectionContentDao,
  rsSessionDao: RsSessionDao,
  rsSessionEventDao: RsSessionEventDao,
  rsSurveyDao: RsSurveyDao,
  rsTimeSpentDiscreteDao: RsTimeSpentDiscreteDao,
  rsTutorialViewDao: RsTutorialViewDao,
  rsUserDao: RsUserDao,
) extends AnalyticsSender:

  import RedshiftEventSender.*

  override def sendAnalytics(
    events: Seq[Analytic],
    busConfig: AnalyticBusConfiguration,
    lastMaterializedViewRefreshDate: Option[Date]
  ): DeliveryResult =

    val result =
      for schemaName <- getSchemaName(busConfig)
      yield
        // this can throw exceptions and `sendAnalytics` mustn't, so we for through
        // a Try because Try.map will catch the bombs
        val didRefreshMaterializedViews = sendAnalyticsUnsafe(
          events,
          schemaName,
          lastMaterializedViewRefreshDate
        )
        DeliveryResult.success(didRefreshMaterializedViews)

    result.toDisjunction.leftMap(permanentFailure).merge
  end sendAnalytics

  private def getSchemaName(busConfiguration: AnalyticBusConfiguration): Try[String] =
    if StringUtils.isBlank(busConfiguration.schemaName) then
      Failure(
        new BusConfigurationError(
          s"RedshiftEventSender requires schemaName in ${AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_CONFIGURATION}"
        )
      )
    else Success(busConfiguration.schemaName.trim())

  private def sendAnalyticsUnsafe(
    events: Seq[Analytic],
    schemaName: String,
    lastMaterializedViewRefreshDate: Option[Date]
  ): Boolean =

    val etlEnv  = EtlEnv(domainDto, TimeSource.fromInstant(Instant.now))
    val payload = events.toList.foldMap(RedshiftTransform.transformEvent)

    if payload.nonEmpty then copyFromS3ToRedshift(payload, schemaName, lastMaterializedViewRefreshDate, etlEnv)
    else false
  end sendAnalyticsUnsafe

  private def copyFromS3ToRedshift(
    records: RedshiftPayload,
    schemaName: String,
    lastMaterializedViewRefreshDate: Option[Date],
    etlEnv: EtlEnv
  ): Boolean =

    val someIO = for
      _ <- records.assets.toNel.traverse(rsAssetDao.insertNew)
      _ <- records.sections.toNel.traverse(rsSectionDao.upsert)
      _ <- records.sectionDeletes.toList.toNel.traverse(rsSectionDao.delete)
      _ <- records.users.toNel.traverse(rsUserDao.upsert)
      _ <- records.userObfuscations.toNel.traverse(rsUserDao.obfuscate)
      _ <- records.sectionContents.toNel.traverse(rsSectionContentDao.set)
      _ <- records.sectionEntries.toNel.traverse(rsSectionEntryDao.insertAll)
      _ <- records.sessions.toNel.traverse(rsSessionDao.insertNew)
      _ <- records.sessionEvents.toNel.traverse(rsSessionEventDao.insertAll)
      _ <- records.grades.values.toList.toNel.traverse(rsGradeDao.upsert)
      _ <- records.unsetGrades.toList.toNel.traverse(rsGradeDao.delete)
      _ <- records.surveyQuestionResponses.toNel.traverse(rsSurveyDao.insertResponses)
      _ <- records.timeSpentDiscrete.toNel.traverse(rsTimeSpentDiscreteDao.insertAll)
      _ <- records.discussionPosts.toNel.traverse(rsDiscussionPostDao.upsert)
      _ <- records.enrollments.toNel.traverse(rsEnrollmentDao.upsert)
      _ <- records.enrollmentDeletes.toList.toNel.traverse(rsEnrollmentDao.delete)
      _ <- records.attempts.toNel.traverse(rsAttemptDao.upsert)
      _ <- records.pageNavEvents.toNel.traverse(rsPageNavEventDao.insertAll)
      _ <- records.progresses.toNel.traverse(rsProgressDao.upsert)
      _ <- records.progressOverTimes.toNel.traverse(rsProgressOverTimeDao.insertAll)
      _ <- records.tutorialViews.toNel.traverse(rsTutorialViewDao.insertAll)
      _ <- records.qnaThreads.toNel.traverse(rsQnaQuestionDao.upsert)
    yield ()

    val allIO = for
      _ <- someIO.local[EtlEnv](_.domainDto)
      _ <- rsActivityDao.insertAll(records.activityKeys)
    yield ()

    val xa = Redshift.buildTransactor(schemaName, useBusUser = true)

    allIO.apply(etlEnv).transact(xa).unsafeRunSync()

    val didRefreshViews = shouldRefreshMaterializedViews(lastMaterializedViewRefreshDate) <|? {
      // timespent has AUTOREFRESH YES, but this bus is so busy that Redshift never gets a chance
      // `select * from SVL_MV_REFRESH_STATUS`:
      // > ... "Refresh failed. Serializable isolation violation." ...
      // incremental refresh takes 1-3 minutes
      //
      // only the table owner can run REFRESH and the bus user is not the owner of any Redshift objects
      // however, REFRESH is not harmful so let the default/main user run it (useBusUser = false)
      log.info("refreshing materialized views")
      val xa = Redshift.buildTransactor(schemaName)
      rsTimeSpentDiscreteDao.refreshTimeSpentView().transact(xa).unsafeRunSync()
    }

    val snapshotIO =
      for _ <- records.instructorSnapshotDays.toNel.traverse(rsInstructorSnapshotDailyDao.generate)
      yield ()

    snapshotIO.transact(xa).unsafeRunSync()

    didRefreshViews
  end copyFromS3ToRedshift

  private def shouldRefreshMaterializedViews(lastRefreshDate: Option[Date]): Boolean =
    lastRefreshDate.map(_.toInstant.plus(1, ChronoUnit.HOURS)).forall(_.isBefore(Instant.now()))
end RedshiftEventSender

object RedshiftEventSender:

  final val RedshiftEventSender                    = "RedshiftEventSender"
  private final implicit val log: org.log4s.Logger = org.log4s.getLogger

  // run sys/script, but choose the right schema name (hint: "loan")
  // loi.cp.analytics.redshift.RedshiftEventSender.createPaused("de")
  def createPaused(schemaName: String): Unit =

    import com.learningobjects.cpxp.scala.cpxp.Summon.summon
    import scaloi.syntax.boolean.*

    val analyticBusService = summon[AnalyticBusService]
    analyticBusService
      .queryBuses()
      .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_SENDER_IDENTIFIER, "eq", RedshiftEventSender)
      .getComponents[AnalyticBus]
      .isEmpty <|? {
      analyticBusService.createBus(
        RedshiftEventSender,
        AnalyticBusState.Paused,
        AnalyticBusConfiguration(maxEvents = 5000, schemaName = schemaName)
      )
    }
  end createPaused
end RedshiftEventSender

private class BusConfigurationError(msg: String) extends RuntimeException(msg) with NoStackTrace
