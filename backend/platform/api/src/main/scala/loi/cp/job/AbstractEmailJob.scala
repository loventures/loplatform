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

package loi.cp.job

import java.io.File
import java.nio.charset.StandardCharsets
import javax.mail.*

import com.github.tototoshi.csv.CSVWriter
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.util.{ManagedUtils, MimeUtils}
import loi.cp.email.MarshalEmailSupport.*
import scala.util.Using
import scaloi.syntax.AnyOps.*

abstract class AbstractEmailJob[J <: EmailJob[J]] extends AbstractJob[J] with EmailJob[J]:
  this: J =>

  val self: EmailJobFacade
  val es: EmailService

  override def update(job: J): J =
    self.setEmailAddresses(job.getEmailAddresses.filter(_.nonEmpty))
    super.update(job)

  override def getEmailAddresses: List[String] = self.getEmailAddresses

  override protected def execute(run: Run): Unit =
    logger.info(s"Generating report ${self.getName}")
    val report = generateReport()
    logger.info(s"Generated report: ${report.subject}")
    report.attachments.foreach(run.attach)

    // commit now, so the attachments are persisted even on email fail
    ManagedUtils.commit()

    val recipients = getEmailAddresses map parseInternetAddress map (_.get) // throw on parse fail

    if report.body.isEmpty && report.attachments.isEmpty then run.succeeded("No report generated.")
    else if recipients.isEmpty then run.succeeded("Report generated. No recipients to send email to.")
    else
      es sendEmail { email =>
        email.setFrom(noreplyAtDomain)
        email.addRecipients(Message.RecipientType.TO, recipients.toArray[Address])
        email.setSubject(report.subject, StandardCharsets.UTF_8.name)
        email.setContent(contentPart(report.body, report.html, report.attachments*))
      }

      run.succeeded(s"Email sent to ${recipients.map(_.getAddress).mkString(", ")}")
    end if
  end execute

  /** Generate the report to be emailed out. */
  protected def generateReport(): GeneratedReport

  /** Create a temporary csv file. */
  protected def csvFile(name: String): UploadInfo = JobUtils.csvFile(name)
end AbstractEmailJob

object JobUtils:

  /** Create a temporary CSV file. */
  def csvFile(name: String): UploadInfo =
    new UploadInfo(
      name,
      MimeUtils.MIME_TYPE_TEXT_CSV + MimeUtils.CHARSET_SUFFIX_UTF_8,
      File.createTempFile("report", ".csv"),
      true
    )

  /** Create and write a temporary CSV file. */
  def csv(name: String)(f: CSVWriter => Unit): UploadInfo =
    csvFile(name) <| { ui =>
      Using.resource(CSVWriter open ui.getFile)(f)
    }
end JobUtils

final case class GeneratedReport(subject: String, body: String, html: Boolean, attachments: Seq[UploadInfo]):
  def this(subject: String, body: String, attachment: UploadInfo) =
    this(subject, body, false, Seq(attachment))
