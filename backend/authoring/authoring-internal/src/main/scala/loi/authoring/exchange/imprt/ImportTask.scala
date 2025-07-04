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

package loi.authoring.exchange.imprt

import java.util.Date

import com.fasterxml.jackson.core.JsonProcessingException
import com.learningobjects.cpxp.jackson.ValidationException
import com.learningobjects.cpxp.util.{GuidUtil, ManagedUtils}
import com.learningobjects.de.task.TaskReport
import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.exchange.imprt.store.ImportReceiptDao
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus
import loi.cp.i18n.{AuthoringBundle, BundleMessage}

abstract class ImportTask[A](
  report: TaskReport,
  receipt: ImportReceipt
)(importReceiptDao: ImportReceiptDao):

  private val log = org.log4s.getLogger

  protected def run(): Option[A]

  final def runSafe(): Option[A] =
    try
      val value = run()
      if value.isEmpty then markFailure()
      report.markComplete(new Date())
      value
    catch case ex: Exception => throw recordFailure(ex)

  private def recordFailure(ex: Exception): Exception =

    val error = ex match
      case ume: UncheckedMessageException => ume.getErrorMessage
      case ve: ValidationException        =>
        AuthoringBundle.message("import.fatalException", GuidUtil.errorGuid(), ve.violations.list.toList.mkString(", "))
      case jpe: JsonProcessingException   =>
        val msg = AuthoringBundle
          .message("import.fatalException", GuidUtil.errorGuid(), jpe.getOriginalMessage)
        log.warn(msg.value)
        log.warn(jpe)("ImportFailure")
        msg
      case exp: Exception                 =>
        val msg = AuthoringBundle
          .message("import.fatalException", GuidUtil.errorGuid(), exp.getMessage)
        log.warn(msg.value)
        log.warn(exp)("Import Failure")
        msg

    report.addError(error)
    markFailure()
    ManagedUtils.commit() // so that rollback doesn't undo our db error recording

    ex
  end recordFailure

  final protected def addError(msg: BundleMessage): Unit = report.addError(msg)

  final protected def hasErrors: Boolean = report.hasErrors

  private def markFailure(): Unit =
    val now = new Date()

    // the receipt report is the overall report
    receipt.report.markComplete(now)

    // this report is a child report of the overall report
    report.markComplete(now)

    receipt.endTime = Some(now)
    receipt.status = AssetExchangeRequestStatus.Failure

    importReceiptDao.merge(ImportReceiptDao.receiptToEntity(receipt))
  end markFailure
end ImportTask
