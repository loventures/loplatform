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

package loi.authoring.exchange.exprt

import java.util.Date

import com.learningobjects.cpxp.util.{GuidUtil, ManagedUtils}
import com.learningobjects.de.task.TaskReport
import loi.authoring.exchange.exprt.store.ExportReceiptDao
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus
import loi.cp.i18n.AuthoringBundle
import scalaz.Validation

abstract class ExportTask[A](
  report: TaskReport,
  receipt: ExportReceipt
)(exportReceiptDao: ExportReceiptDao):

  private val log = org.log4s.getLogger

  protected def run(): Validation[Throwable, A]

  final def runSafe(): Validation[Throwable, A] =
    val value = run()
    report.markComplete(new Date())
    value.leftMap(recordFailure)

  private def recordFailure(ex: Throwable): Throwable =
    val msg = AuthoringBundle.message("export.fatalException", GuidUtil.errorGuid(), ex.getMessage)
    log.warn(msg.value)
    log.warn(ex)("Export Failure")

    report.addError(msg)
    markFailure()
    ManagedUtils.commit() // so that rollback doesn't undo our db error recording

    ex
  end recordFailure

  private def markFailure(): Unit =
    val now = new Date()

    // the receipt report is the overall report
    receipt.report.markComplete(now)

    // this report is a child report of the overall report
    report.markComplete(now)

    receipt.endTime = Some(now)
    receipt.status = AssetExchangeRequestStatus.Failure

    exportReceiptDao.update(ExportReceiptDao.receiptToEntity(receipt))
  end markFailure
end ExportTask
