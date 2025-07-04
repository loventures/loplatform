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

package loi.authoring.copy

import java.util.Date

import com.learningobjects.de.task.{TaskReport, TaskReportService}
import loi.cp.i18n.BundleMessage

case class CopyReceipt(
  id: Long,
  originalId: Long,
  copyId: Option[Long],
  report: TaskReport,
  status: CopyReceiptStatus,
  createTime: Date,
  startTime: Option[Date],
  endTime: Option[Date],
  createdBy: Option[Long],
  domainId: Long
):

  def markStart(): CopyReceipt =
    val now           = new Date()
    val startedReport = report.copy()
    startedReport.markStart(now)
    TaskReportService.track(startedReport)
    copy(
      report = startedReport,
      startTime = Some(now),
      status = CopyReceiptStatus.UNDERWAY
    )
  end markStart

  def markProgress(progressUnits: Int): CopyReceipt =
    val markedReport = report.copy()
    markedReport.markProgress(progressUnits)
    TaskReportService.track(markedReport)
    copy(report = markedReport)

  def markSuccess(targetId: Long): CopyReceipt =
    markComplete().copy(
      status = CopyReceiptStatus.SUCCESS,
      copyId = Some(targetId)
    )

  def markFailure(): CopyReceipt =
    markComplete().copy(status = CopyReceiptStatus.FAILURE)

  private def markComplete(): CopyReceipt =
    val now             = new Date()
    val completedReport = report.copy()
    completedReport.markComplete(now)
    TaskReportService.track(completedReport)
    copy(
      report = completedReport,
      endTime = Some(now)
    )

  def addError(msg: BundleMessage): CopyReceipt =
    val erroredReport = report.copy()
    erroredReport.addError(msg)
    TaskReportService.track(erroredReport)
    copy(report = erroredReport)
end CopyReceipt
