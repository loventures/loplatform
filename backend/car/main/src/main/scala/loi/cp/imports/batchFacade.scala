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

import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.{Facade, FacadeComponent, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.attachment.AttachmentFacade
import com.learningobjects.cpxp.service.component.misc.BatchConstants
import com.learningobjects.cpxp.service.folder.FolderConstants
import com.learningobjects.cpxp.service.query.QueryBuilder

import java.lang.Long as JLong
import java.util.Date as JDate

@FacadeItem(BatchConstants.ITEM_TYPE_BATCH)
trait ImportFacade extends Facade:

  @FacadeData
  def getIdentifier: String
  def setIdentifier(identifier: String): Unit

  @FacadeData
  def getFailureCount: JLong
  def setFailureCount(failureCount: JLong): Unit

  @FacadeData
  def getSuccessCount: JLong
  def setSuccessCount(successCount: JLong): Unit

  @FacadeData
  def getTotal: JLong
  def setTotal(successCount: JLong): Unit

  @FacadeData
  def getCallbackUrl: String
  def setCallbackUrl(callbackUrl: String): Unit

  @FacadeData
  def getCreateTime: JDate
  def setCreateTime(createTime: JDate): Unit

  @FacadeData
  def getStartTime: JDate
  def setStartTime(startTime: JDate): Unit

  @FacadeData
  def getEndTime: JDate
  def setEndTime(endTime: JDate): Unit

  @FacadeData
  def getUserStarted: Option[Long]
  def setUserStarted(userStarted: Option[Long]): Unit

  @FacadeData
  def getStatus: ImportStatus
  def setStatus(status: ImportStatus): Unit

  @FacadeData
  def getType: Option[String]
  def setType(`type`: Option[String]): Unit

  @FacadeData
  def getImportFile: AttachmentFacade
  def setImportFile(uploadInfo: UploadInfo): Unit
end ImportFacade

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
trait ImportParentFacade extends Facade:
  @FacadeComponent
  def addImport[T <: ImportComponent](batch: T): T
  def addImport(): ImportComponent
  def getImport(id: JLong): Option[ImportComponent]
  def getImports: List[ImportComponent]
  def queryImports: QueryBuilder
