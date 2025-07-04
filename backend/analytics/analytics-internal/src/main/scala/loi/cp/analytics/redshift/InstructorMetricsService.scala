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

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import com.learningobjects.cpxp.service.query.Comparison
import doobie.*
import doobie.implicits.*
import loi.cp.analytics.bus.{AnalyticBusFacade, AnalyticBusService, AnalyticBusState}
import loi.db.Redshift
import loi.doobie.log.*
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*

import java.sql.Timestamp

@Service
class InstructorMetricsService(
  analyticsBusService: AnalyticBusService,
  ts: TimeSource,
):
  import InstructorMetricsService.*

  def fetchTransactor(): String \/ Transactor[IO] = for
    bus <- analyticsBusService
             .queryBuses()
             .addCondition(
               AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_SENDER_IDENTIFIER,
               Comparison.eq,
               RedshiftEventSender.RedshiftEventSender
             )
             .getFacades[AnalyticBusFacade]
             .headOption
             .toRightDisjunction("no Redshift bus in domain")
    _   <- (bus.getState == AnalyticBusState.Active).elseLeft(s"bus ${bus.getId} is not active; state: ${bus.getState}")
  yield Redshift.buildTransactor(bus.getConfiguration.schemaName)

  def selectAwolInstructors(
    xa: Transactor[IO],
    excludeEmails: List[String],
    excludeEmailDomains: List[String]
  ): List[AwolInstructor] =
    val allInstructors = selectAwolInstructorsIO.transact(xa).unsafeRunSync()
    allInstructors.filterNot(exclude(excludeEmails, excludeEmailDomains, _.instructorEmail))

  // the `mrie.daylag IS NULL` is for when the instructor _never_ entered the section
  private val selectAwolInstructorsIO: ConnectionIO[List[AwolInstructor]] =
    sql"""SELECT
         |  u.email,
         |  s.name,
         |  mrie.mostrecententry,
         |  mrie.daylag,
         |  u.externalid,
         |  s.externalid
         |FROM mostrecentinstructorentry mrie
         |  LEFT JOIN usr u ON mrie.userid = u.id
         |  LEFT JOIN section s ON mrie.sectionid = s.id
         |WHERE mrie.daylag > 2 OR mrie.daylag IS NULL
         |ORDER BY mrie.daylag DESC
         |LIMIT 2000
       """.stripMargin.query[AwolInstructor].to[List]

  def selectOverdueAttempts(
    xa: Transactor[IO],
    excludeEmails: List[String],
    excludeEmailDomains: List[String]
  ): List[OverdueAttempt] =
    val allAttempts = selectOverdueAttemptsIO.transact(xa).unsafeRunSync()
    allAttempts.filterNot(exclude(excludeEmails, excludeEmailDomains, _.instructorEmail))

  // The ETL ought to ensure that these left joins always match, making them inner joins
  // in practice. But in case of a bug, I would rather report attempts with missing data
  // than not report the attempt at all.
  // using Redshift function `getdate()` works too but leads to flakey tests, so the TimeSource's
  // current time is used instead
  private val selectOverdueAttemptsIO: ConnectionIO[List[OverdueAttempt]] =
    sql"""SELECT
         |  instructoruser.email,
         |  section.name,
         |  learneruser.email,
         |  asset.title,
         |  overdueattempt.submittime,
         |  date_diff('hour', overdueattempt.submittime, ${ts.timestamp}),
         |  overdueattempt.id,
         |  instructoruser.externalid,
         |  section.externalid,
         |  learneruser.externalid
         |FROM overdueattempt
         |  LEFT JOIN usr instructoruser on overdueattempt.instructoruserid = instructoruser.id
         |  LEFT JOIN usr learneruser on overdueattempt.learneruserid = learneruser.id
         |  LEFT JOIN section on overdueattempt.sectionid = section.id
         |  LEFT JOIN asset on overdueattempt.assetid = asset.id
         |ORDER BY section.externalid, instructoruser.externalid, overdueattempt.submittime
         |LIMIT 2000
         |""".stripMargin.query[OverdueAttempt].to[List]

  // TODO switch to using plain usr.emaildomain column
  private def exclude[A](
    excludeEmails: List[String],
    excludeEmailDomains: List[String],
    instructorEmail: A => Option[String]
  )(row: A): Boolean =
    instructorEmail(row).exists(email =>
      val domain = email.split('@').last
      excludeEmails.contains(email) || excludeEmailDomains.contains(domain)
    )
end InstructorMetricsService

object InstructorMetricsService:
  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  case class OverdueAttempt(
    instructorEmail: Option[String],
    sectionName: Option[String],
    learnerEmail: Option[String],
    assetTitle: Option[String],
    submitTime: Option[Timestamp],
    awaitingScoreHours: Option[Int],
    attemptId: Long,
    instructorId: Option[String],
    sectionId: Option[String],
    learnerId: Option[String],
  ):
    val fields: List[String] = List(
      instructorEmail.orNull,
      sectionName.orNull,
      learnerEmail.orNull,
      assetTitle.orNull,
      submitTime.map(RedshiftSchema.RedshiftDateFormat.format).orNull,
      awaitingScoreHours.map(_.toString).orNull,
      attemptId.toString,
      instructorId.orNull,
      sectionId.orNull,
      learnerId.orNull,
    )
  end OverdueAttempt

  object OverdueAttempt:
    val Headers: List[String] = List(
      "Instructor Email",
      "Section Name",
      "Learner Email",
      "Asset Title",
      "Submit Time",
      "Score Wait Time (Hours)",
      "Attempt ID",
      "Instructor ID",
      "Section ID",
      "Learner ID",
    )
  end OverdueAttempt

  case class AwolInstructor(
    instructorEmail: Option[String],
    sectionName: Option[String],
    mostRecentEntryTime: Option[Timestamp],
    dayLag: Option[Int],
    instructorId: Option[String],
    sectionId: Option[String],
  ):
    val fields: List[String] = List(
      instructorEmail.orNull,
      sectionName.orNull,
      mostRecentEntryTime.map(RedshiftSchema.RedshiftDateFormat.format).orNull,
      dayLag.map(_.toString).orNull,
      instructorId.orNull,
      sectionId.orNull
    )
  end AwolInstructor

  object AwolInstructor:
    val Headers: List[String] = List(
      "Instructor Email",
      "Section Name",
      "Most Recent Entry Time (UTC)",
      "Days Ago",
      "Instructor ID",
      "Section ID",
    )
end InstructorMetricsService
