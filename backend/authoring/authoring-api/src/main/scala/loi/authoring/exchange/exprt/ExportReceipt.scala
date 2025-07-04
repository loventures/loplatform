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

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.de.task.TaskReport
import loi.authoring.blob.BlobRef
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus

import java.util.Date

/** @param data
  *   data about the export (export name, branch name, node names)
  */
class ExportReceipt(
  val id: Long,
  val data: JsonNode,
  var source: Option[BlobRef],
  val report: TaskReport,
  var attachmentId: Option[Long],
  var status: AssetExchangeRequestStatus,
  val createTime: Date,
  var startTime: Option[Date],
  var endTime: Option[Date],
  val createdBy: Option[Long],
  val domainId: Long
):

  private val om = JacksonUtils.getFinatraMapper

  def markStart(): Unit =
    val now = new Date()
    report.markStart(now)
    startTime = Some(now)
    status = AssetExchangeRequestStatus.Underway

  def markSuccess(exportBlob: BlobRef): Unit =
    val now = new Date()
    report.markComplete(now)
    endTime = Some(now)
    status = AssetExchangeRequestStatus.Success
    source = Option(exportBlob)

  // because this class is mutable but needs to act immutably in one spot
  def copy(): ExportReceipt =
    new ExportReceipt(
      id,
      data,
      source,
      report.copy(),
      attachmentId,
      status,
      createTime,
      startTime,
      endTime,
      createdBy,
      domainId
    )
end ExportReceipt
