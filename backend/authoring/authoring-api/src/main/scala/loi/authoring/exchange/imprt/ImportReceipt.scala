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

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.de.task.TaskReport
import loi.authoring.blob.BlobRef
import loi.authoring.exchange.model.ImportedRoot
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus

import java.util.Date

class ImportReceipt(
  val id: Long,
  var data: JsonNode,
  var importedRoots: Seq[ImportedRoot],
  var report: TaskReport,
  val attachmentId: Option[Long],
  var downloadFilename: Option[String],
  var status: AssetExchangeRequestStatus,
  val createTime: Date,
  var startTime: Option[Date],
  var endTime: Option[Date],
  val createdBy: Option[Long],
  val domainId: Long,
  var source: Option[BlobRef],           // the LO `exchange` conversion
  val unconvertedSource: Option[BlobRef] // the original zip, pre-LO conversion
):

  // because this class is mutable but needs to act immutably in one spot
  def copy(): ImportReceipt =
    new ImportReceipt(
      id,
      data,
      importedRoots,
      report.copy(),
      attachmentId,
      downloadFilename,
      status,
      createTime,
      startTime,
      endTime,
      createdBy,
      domainId,
      source,
      unconvertedSource
    )

  def updated(
    newConvertedSource: BlobRef,
    newData: JsonNode,
    newImportStatus: AssetExchangeRequestStatus
  ): ImportReceipt =
    new ImportReceipt(
      id,
      newData,
      importedRoots,
      report,
      attachmentId,
      downloadFilename,
      newImportStatus,
      createTime,
      startTime,
      endTime,
      createdBy,
      domainId,
      Some(newConvertedSource),
      unconvertedSource
    )
end ImportReceipt
