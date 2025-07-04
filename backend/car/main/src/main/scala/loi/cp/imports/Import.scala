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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport}
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentService,
  ComponentSupport
}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.{UserDTO, UserWebService}
import com.learningobjects.cpxp.util.InternationalizationUtils
import loi.cp.attachment.AttachmentComponent
import loi.cp.imports.errors.{GenericError, ImportErrorComponent}

import java.lang.Long as JLong
import java.util.Date as JDate

@Component
class Import(
  val componentInstance: ComponentInstance,
  self: ImportFacade,
)(implicit cs: ComponentService, fs: FacadeService, uws: UserWebService)
    extends ImportComponent
    with ComponentImplementation:
  override def getId: JLong = self.getId

  override def getIdentifier: String                   = self.getIdentifier
  override def setIdentifier(identifier: String): Unit =
    self.setIdentifier(identifier)

  override def getCallbackUrl: Option[String]                    =
    Option.apply(self.getCallbackUrl)
  override def setCallbackUrl(callbackUrl: Option[String]): Unit =
    callbackUrl.fold(())(c => self.setCallbackUrl(c))

  override def getCreateTime: JDate                   = self.getCreateTime
  override def setCreateTime(createTime: JDate): Unit =
    self.setCreateTime(createTime)

  override def getStartTime: JDate                  = self.getStartTime
  override def setStartTime(startTime: JDate): Unit =
    self.setStartTime(startTime)

  override def getEndTime: JDate                = self.getEndTime
  override def setEndTime(endTime: JDate): Unit = self.setEndTime(endTime)

  override def getStatus: ImportStatus               = self.getStatus
  override def setStatus(status: ImportStatus): Unit = self.setStatus(status)

  override def getSuccessCount: JLong                     = self.getSuccessCount
  override def setSuccessCount(successCount: JLong): Unit =
    self.setSuccessCount(successCount)
  override def incrementSuccesses(): Unit                 =
    self.setSuccessCount(self.getSuccessCount + 1L)
  override def addSuccess(request: ImportItem): Unit      = incrementSuccesses()

  override def getTotal: JLong              = self.getTotal
  override def setTotal(total: JLong): Unit = self.setTotal(total)

  override def getStartedBy: Option[UserDTO] =
    self.getUserStarted.map(uws.getUserDTO(_))

  override def setStartedBy(startedBy: Option[UserDTO]): Unit =
    self.setUserStarted(startedBy.map(_.id))

  override def getType: Option[String] =
    self.getType.map(t =>
      Option(ComponentSupport.lookup(classOf[Importer[? <: ImportItem]], Class.forName(t)))
        .flatMap(i => Option(i.getComponentInstance.getComponent.getAnnotation(classOf[ImportBinding])))
        .map(b => InternationalizationUtils.formatMessage(b.label))
        .getOrElse("Unknown Type")
    )

  override def setType(`type`: Option[String]): Unit = self.setType(`type`)

  override def getFailureCount: JLong = self.getFailureCount

  override def setFailureCount(failureCount: JLong): Unit =
    self.setFailureCount(failureCount)
  def incrementFailures(): Unit                           =
    self.setFailureCount(self.getFailureCount + 1L)

  override def addFailure(index: Long)(err: GenericError): Unit =
    val batchError = self.facade[ImportErrorParentFacade].addBatchError()
    batchError.setError(err)
    batchError.setIndex(index)
    incrementFailures()

  override def addSuccess(success: ImportSuccess): Unit = incrementSuccesses()

  override def getErrors(apiQuery: ApiQuery): ApiQueryResults[ImportErrorComponent] =
    val qb = self.facade[ImportErrorParentFacade].queryBatchErrors
    ApiQuerySupport.query(qb, apiQuery, classOf[ImportErrorComponent])

  override def setImportFileFromUpload(uploadInfo: UploadInfo): Unit =
    self.setImportFile(uploadInfo)
  override def getImportFile: Option[AttachmentComponent]            =
    Option(self.getImportFile).map(_.getId.component[AttachmentComponent])

  override def delete(): Unit = self.delete()
end Import
