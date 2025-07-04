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

package loi.cp.usage

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.{ZoneId, ZonedDateTime}

import com.github.tototoshi.csv.CSVWriter
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentInstance
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.util.{HtmlOps, HtmlTemplate}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import scaloi.syntax.AnyOps.*
import com.learningobjects.cpxp.service.domain.{DomainFacade, DomainState}
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.UserWebService
import loi.cp.job.{AbstractEmailJob, EmailJobFacade, GeneratedReport}
import scala.util.Using

import scala.jdk.CollectionConverters.*
import scalaz.std.iterable.*
import scalaz.syntax.foldable.*

/** Cluster usage report job. Pulls usage information from the database for each domain and then emails this out as text
  * and CSV.
  */
@Component
class ClusterUsageReportImpl(
  val componentInstance: ComponentInstance,
  val self: EmailJobFacade,
  val es: EmailService,
  val fs: FacadeService,
  implicit val qs: QueryService,
  implicit val uws: UserWebService,
  ows: OverlordWebService,
  sm: ServiceMeta
) extends AbstractEmailJob[ClusterUsageReport]
    with ClusterUsageReport:
  import ClusterUsageReportImpl.*

  /** Generate the report to be emailed out. */
  override protected def generateReport(): GeneratedReport =
    // report as "yesterday", assuming running in the middle of the night
    val now   = currentTime
    val end   = now.`with`(ChronoField.SECOND_OF_DAY, 0)
    val start = end.minusHours(12).`with`(ChronoField.SECOND_OF_DAY, 0)

    // pair all the domains with their usage
    val usages     = domains map { domain =>
      domain -> DomainUsageInfoCalculator.calculate(domain, now.toInstant)
    }
    // sum all the domain usages
    val totalUsage = usages.map(_._2).suml

    val subject  =
      s"Cluster usage report - ${sm.getCluster} - ${DateOnly.format(start)}"
    val template = HtmlTemplate(this, classOf[ClusterUsageReportImpl], "clusterUsageEmail.html").bind(
      "serviceMeta" -> sm,
      "date"        -> DateOnly.format(start),
      "dateTime"    -> DateAndTime.format(now),
      "totalUsage"  -> totalUsage,
      "usages"      -> usages,
      "headers"     -> DomainUsageInfo.Headers
    )
    val body     = HtmlOps.render(template)

    GeneratedReport(subject, body, html = true, List(clusterReport(start, usages)))
  end generateReport

  /** Get a cluster report CSV. */
  private def clusterReport(start: ZonedDateTime, usages: Seq[(DomainFacade, DomainUsageInfo)]): UploadInfo =
    csvFile(s"${sm.getCluster}_${DateFilename.format(start)}.csv") <| { report =>
      Using.resource(CSVWriter open report.getFile) { csv =>
        csv writeRow Headers ++ DomainUsageInfo.Headers
        usages foreach { usage =>
          var prefix = List(usage._1.getName, sm.getCluster, DateOnly.format(start))
          csv writeRow prefix ++ usage._2.productIterator
        }
      }
    }

  /** Domain states upon which to report. */
  private val NormalStates = Set(DomainState.Normal, DomainState.Maintenance)

  /** Get the domains to consider. */
  private val domains: Seq[DomainFacade] =
    ows.getAllDomains.asScala.toSeq.filter(d => NormalStates.contains(d.getState))

  /** Get the current time and timezone. Overlord doesn't have a timezone configured. */
  def currentTime: ZonedDateTime =
    ZonedDateTime.now(ZoneId.of("America/New_York"))

  /** The logger. */
  override val logger = org.log4s.getLogger
end ClusterUsageReportImpl

/** Cluster usage report job companion.
  */
object ClusterUsageReportImpl:

  /** Format just the date. */
  private val DateOnly = DateTimeFormatter.ofPattern("yyyy/MM/dd")

  /** Format the date and time. */
  private val DateAndTime =
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z")

  /** Format a date for use in a filename. */
  private val DateFilename = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /** Additional headers. */
  private val Headers = List("Domain", "Cluster", "Date")
end ClusterUsageReportImpl
