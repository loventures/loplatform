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

package loi.cp.imports

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonProperty}
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{ItemMapping, RequestMapping, Schema}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.component.misc.BatchConstants
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.attachment.AttachmentComponent
import loi.cp.imports.errors.{GenericError, ImportErrorComponent}

import java.lang.Long as JLong
import java.util.Date as JDate

@ItemMapping(value = BatchConstants.ITEM_TYPE_BATCH, singleton = true)
@Schema("batch")
@JsonIgnoreProperties(ignoreUnknown = true)
trait ImportComponent extends ComponentInterface with Id:

  @JsonProperty
  def getIdentifier: String
  def setIdentifier(identifier: String): Unit
  @JsonProperty
  def getFailureCount: JLong
  def setFailureCount(failureCount: JLong): Unit
  @JsonProperty
  def getSuccessCount: JLong
  def setSuccessCount(successCount: JLong): Unit
  @JsonProperty
  def getTotal: JLong
  def setTotal(total: JLong): Unit
  def incrementSuccesses(): Unit
  @JsonProperty
  def getCallbackUrl: Option[String]
  def setCallbackUrl(callbackUrl: Option[String]): Unit
  @JsonProperty
  def getCreateTime: JDate
  def setCreateTime(createTime: JDate): Unit
  @JsonProperty
  def getStartTime: JDate
  def setStartTime(startTime: JDate): Unit
  @JsonProperty
  def getEndTime: JDate
  def setEndTime(endTime: JDate): Unit

  // TODO: hopefully we can unite these two eventually
  @JsonProperty
  def getStartedBy: Option[UserDTO]
  def setStartedBy(startedBy: Option[UserDTO]): Unit

  @JsonIgnore
  def setImportFileFromUpload(uploadInfo: UploadInfo): Unit
  @JsonProperty
  @RequestMapping(path = "file", method = Method.GET)
  def getImportFile: Option[AttachmentComponent]

  @JsonProperty
  def getType: Option[String]
  def setType(`type`: Option[String]): Unit

  @JsonProperty
  def getStatus: ImportStatus
  def setStatus(status: ImportStatus): Unit

  @RequestMapping(path = "errors", method = Method.GET)
  def getErrors(apiQuery: ApiQuery): ApiQueryResults[ImportErrorComponent]

  def addFailure(index: Long)(err: GenericError): Unit
  def addSuccess(request: ImportItem): Unit
  def addSuccess(success: ImportSuccess): Unit

  @RequestMapping(method = Method.DELETE) // this only exists for the integration tests?
  def delete(): Unit
end ImportComponent
