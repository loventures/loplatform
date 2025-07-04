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

package loi.cp.usage.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.ComponentInstance
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.scala.util.Stopwatch.profiled
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.{QueryService, BaseDataProjection as Proj, Comparison as CMP}
import com.learningobjects.cpxp.usage.DomainStatsFinder.*
import com.learningobjects.cpxp.util.JDBCUtils.doQuery
import com.learningobjects.cpxp.util.ManagedUtils
import loi.cp.job.*
import loi.cp.usage.DomainStatisticsType.*
import loi.cp.usage.{DomainStatisticsFacade, DomainStatisticsType, UsageServlet}
import org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase
import org.log4s.Logger
import scaloi.syntax.FiniteDurationOps.*

import java.text.{NumberFormat, SimpleDateFormat}
import java.time.temporal.ChronoField.*
import java.time.{Period, ZoneOffset, ZonedDateTime}
import java.util.Date
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Component
class GenerateDailyDomainStatisticsImpl(
  val componentInstance: ComponentInstance,
  val self: EmailJobFacade,
  val es: EmailService,
  val fs: FacadeService,
  overlordWebService: OverlordWebService,
  queryService: QueryService,
  mapper: ObjectMapper,
) extends AbstractEmailJob[GenerateDailyDomainStatistics]
    with GenerateDailyDomainStatistics:
  import GenerateDailyDomainStatisticsImpl.*

  private lazy val today = ZonedDateTime.now(ZoneOffset.UTC).`with`(NANO_OF_DAY, 0)

  override protected def generateReport(): GeneratedReport =
    val minDate  = today.minus(BackFillPeriod)
    val statDays = this.statDays(minDate)

    lazy val pastDays: LazyList[ZonedDateTime] =
      today.minusDays(1) #:: pastDays.map(_.minusDays(1))

    val watch   = new Stopwatch
    val results = pastDays
      .takeWhile(!_.isBefore(minDate))
      .map { d =>
        (AllStatTypes.diff(statDays.getOrElse(d.toInstant, Set.empty)), d)
      }
      .filter { case (missingStats, _) => missingStats.nonEmpty }
      .take(MaxDaysToCalcPerRun)
      .map { case (stats, d) => (Date.from(d.toInstant), generateStats(stats, d)) }

    val (hasErrors, hasSuccesses) = (
      results.exists(_._2.isLeft),
      results.exists(_._2.isRight)
    )

    val status =
      if !hasErrors then "SUCCESS"
      else s"${if hasSuccesses then "PARTIAL " else ""}FAILURE"

    val jobName = splitByCharacterTypeCamelCase(
      classOf[GenerateDailyDomainStatistics].getSimpleName
    ).mkString(" ")

    val body = s"""
         |Duration: ${watch.elapsed.toHumanString}
         |
         |Results:
         |${results.map { case (d, r) => formatResult(d, r) }.mkString("\n")}
       """.stripMargin

    GeneratedReport(
      subject = s"$status $jobName - ${java.time.Instant.now}",
      body = body,
      html = false,
      attachments = Nil,
    )
  end generateReport

  private lazy val overLordId = overlordWebService.findOverlordDomainId().toLong

  private def generateStats(
    missingStats: Set[DomainStatisticsType],
    day: ZonedDateTime
  ) =
    Try {
      import java.time.Instant

      val start: Instant = day.toInstant
      val end: Instant   = day.plusDays(1).toInstant

      val statAccums = mutable.Map[(Long, DomainStatisticsType), StatCounter]()
      val query      =
        s"""
         |SELECT root_id, datajson
         |FROM analyticfinder
         |WHERE
         |  time >= '$start'
         |  AND time < '$end'
         |ORDER BY time ASC
      """.stripMargin

      var rowCount      = 0
      val (_, duration) = profiled {
        doQuery(query, disableCache = true) { rs =>
          while rs.next() do
            rowCount += 1
            val data = rs.getString("datajson")
            if data != null then
              val json = mapper.readTree(data.getBytes)
              missingStats.foreach { statsType =>
                val key = (rs.getLong("root_id"), statsType)
                statAccums.getOrElseUpdate(key, counter(statsType)).process(json)
              }
        }

        statAccums.foreach { case ((domainId, statsType), counter) =>
          domainId.addFacade[DomainStatisticsFacade] { stats =>
            stats.setType(statsType.entryName)
            stats.setTime(start)
            stats.setValue(counter.currentTotal)
          }
        }
        ManagedUtils.commit()
      }

      val totals = statAccums
        .map { case ((_, stat), counter) => (stat, counter) }
        .groupBy(_._1)
        .view
        .mapValues(_.values.toSeq)
        .map { case (stat, counters) => stat -> sum(counters) }
        .toMap

      totals.keys.filter(_ == DistinctUsers).foreach { k =>
        overLordId.addFacade[DomainStatisticsFacade] { stats =>
          stats.setType(UsageServlet.TotalDistinctUsers)
          stats.setTime(start)
          stats.setValue(totals(k))
        }
      }

      Success(rowCount, duration, totals)
    }.toEither.left.map(_.getMessage)

  private def statDays(minDate: ZonedDateTime) =
    queryService
      .queryAllDomains(ITEM_TYPE_DAILY_DOMAIN_STATISTICS)
      .setDataProjection(Proj.ofData(DATA_TYPE_TYPE, DATA_TYPE_TIME))
      .addCondition(DATA_TYPE_TIME, CMP.ge, Date.from(minDate.toInstant))
      .addCondition(DATA_TYPE_TIME, CMP.lt, Date.from(today.toInstant))
      .setDistinct(true)
      .getResultList[Any]
      .asScala
      .flatMap { case Array(st: String, d: Date) =>
        DomainStatisticsType
          .withNameOption(st)
          .map(
            (
              ZonedDateTime
                .ofInstant(d.toInstant, ZoneOffset.UTC)
                .`with`(NANO_OF_DAY, 0)
                .toInstant,
              _
            )
          )
      }
      .groupBy { case (t, _) => t }
      .view
      .mapValues(_.map(_._2).toSet)
      .toMap

  private def formatResult(d: Date, res: Either[String, Success]) =
    val ds = DateFmt.format(d)
    if res.isRight then
      val sc = res.toOption.get
      if sc.rowCount > 0 then
        val rows    = CountFmt.format(sc.rowCount)
        val dur     = sc.duration.toHumanString
        val details = sc.stats
          .map { case (k, v) =>
            val stat = splitByCharacterTypeCamelCase(k.entryName).map(_.toLowerCase).mkString(" ")
            val rows = CountFmt.format(v)
            s"$stat -> $rows"
          }
          .mkString(", ")
        s""" - $ds - success: examined $rows events in $dur
           |    $details""".stripMargin
      else ""
      end if
    else s" - $ds - error: ${res.swap.toOption.get}"
    end if
  end formatResult

  private implicit val fsi: FacadeService = fs

  override protected val logger: Logger = org.log4s.getLogger
end GenerateDailyDomainStatisticsImpl

private object GenerateDailyDomainStatisticsImpl:
  lazy val AllStatTypes: Set[DomainStatisticsType] =
    DomainStatisticsType.values.toSet

  val MaxDaysToCalcPerRun: Int = 365 / 2
  val BackFillPeriod: Period   = Period.ofYears(2)

  val SubjectFmt             = s"[%s] %s %s - %s"
  val CountFmt: NumberFormat = NumberFormat.getNumberInstance
  val DateFmt                = new SimpleDateFormat("yyyy-MM-dd")

  case class Success(
    rowCount: Int,
    duration: FiniteDuration,
    stats: Map[DomainStatisticsType, Long]
  )
end GenerateDailyDomainStatisticsImpl
