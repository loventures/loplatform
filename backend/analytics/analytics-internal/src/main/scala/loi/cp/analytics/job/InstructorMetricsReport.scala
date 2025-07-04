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

package loi.cp.analytics.job

import cats.effect.IO
import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentInstance
import com.learningobjects.cpxp.component.annotation.{Component, Schema}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import doobie.Transactor
import loi.cp.analytics.redshift.InstructorMetricsService
import loi.cp.job.{AbstractEmailJob, EmailJob, GeneratedReport, JobUtils}
import org.log4s.Logger

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

@Schema("instructorMetricsReport")
trait InstructorMetricsReport extends EmailJob[InstructorMetricsReport]:

  @JsonProperty
  def getExcludeEmails: List[String]
  def setExcludeEmails(excludeEmails: List[String]): Unit

  @JsonProperty
  def getExcludeEmailDomains: List[String]
  def setExcludeEmailDomains(excludeEmailDomains: List[String]): Unit
end InstructorMetricsReport

@Component
class InstructorMetricsReportImpl(
  val componentInstance: ComponentInstance,
  val fs: FacadeService,
  val es: EmailService,
  val self: InstructorMetricsReportFacade,
  instructorMetricsService: InstructorMetricsService,
  sm: ServiceMeta
) extends AbstractEmailJob[InstructorMetricsReport]
    with InstructorMetricsReport:

  import InstructorMetricsReportImpl.*

  override val logger: Logger = org.log4s.getLogger

  override def getExcludeEmails: List[String]                                  = self.getExcludeEmails
  override def setExcludeEmails(excludeEmails: List[String]): Unit             = self.setExcludeEmails(excludeEmails)
  override def getExcludeEmailDomains: List[String]                            = self.getExcludeEmailDomains
  override def setExcludeEmailDomains(excludeEmailDomains: List[String]): Unit =
    self.setExcludeEmailDomains(excludeEmailDomains)

  override def update(job: InstructorMetricsReport): InstructorMetricsReport =
    setExcludeEmails(job.getExcludeEmails)
    setExcludeEmailDomains(job.getExcludeEmailDomains)
    super.update(job)

  override protected def generateReport(): GeneratedReport =

    val xa                  = instructorMetricsService.fetchTransactor().valueOr(error => throw new RuntimeException(error))
    val time                = DateFilename.format(ZonedDateTime.now(ZoneId.systemDefault()))
    val excludeEmails       = getExcludeEmails
    val excludeEmailDomains = getExcludeEmailDomains

    val awolReport            = generateAwolReport(xa, time, excludeEmails, excludeEmailDomains)
    val overdueAttemptsReport = generateOverdueAttemptsReport(xa, time, excludeEmails, excludeEmailDomains)

    GeneratedReport(
      s"Instructor Metrics Report ${sm.getCluster} $time",
      "",
      html = false,
      List(awolReport, overdueAttemptsReport)
    )
  end generateReport

  private def generateAwolReport(
    xa: Transactor[IO],
    time: String,
    excludeEmails: List[String],
    excludeEmailDomains: List[String]
  ): UploadInfo =
    val awolInstructors = instructorMetricsService.selectAwolInstructors(xa, excludeEmails, excludeEmailDomains)
    JobUtils.csv(s"instructor-awol-${sm.getCluster}-$time.csv")(csv =>
      csv.writeRow(InstructorMetricsService.AwolInstructor.Headers)
      awolInstructors.foreach(row => csv.writeRow(row.fields))
    )
  end generateAwolReport

  private def generateOverdueAttemptsReport(
    xa: Transactor[IO],
    time: String,
    excludeEmails: List[String],
    excludeEmailDomains: List[String]
  ): UploadInfo =
    val overdueAttempts = instructorMetricsService.selectOverdueAttempts(xa, excludeEmails, excludeEmailDomains)
    JobUtils.csv(s"instructor-overdue-attempts-${sm.getCluster}-$time.csv")(csv =>
      csv.writeRow(InstructorMetricsService.OverdueAttempt.Headers)
      overdueAttempts.foreach(row => csv.writeRow(row.fields))
    )
  end generateOverdueAttemptsReport
end InstructorMetricsReportImpl

object InstructorMetricsReportImpl:
  private val DateFilename = DateTimeFormatter.ofPattern("yyyy-MM-dd")
