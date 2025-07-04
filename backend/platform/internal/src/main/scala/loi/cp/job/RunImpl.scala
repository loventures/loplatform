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

import java.lang.Long as jLong
import java.util.Date

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Service.*
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.util.ManagedUtils
import loi.cp.attachment.AttachmentViewComponent

@Component
class RunImpl(val componentInstance: ComponentInstance, self: RunFacade) extends Run with ComponentImplementation:
  @PostCreate
  private def init(): Unit = self.setStartTime(new Date)

  override def getId: jLong = self.getId

  override def getStartTime: Date = self.getStartTime

  override def getEndTime: Option[Date] = self.getEndTime

  override def getSuccess: Option[Boolean] = self.getSuccess

  override def getReason: Option[String] = self.getReason

  // Internal calls to asComponent don't work for subclasses of ComponentMixin
  // because scala creates several methods for Java vararg methods, the component
  // framework only implements one of them and the compiler binds to the wrong one.
  override def getAttachments: AttachmentViewComponent =
    getComponentInstance.getInstance(classOf[AttachmentViewComponent])

  override def attach(upload: UploadInfo): Unit =
    service[AttachmentWebService].createAttachment(getId, upload)
    ()

  override def succeeded(reason: String): Unit =
    ManagedUtils.commit() // this is wonky but clean
    complete(success = true, reason)

  override def failed(reason: String): Unit =
    ManagedUtils.rollback() // this is wonky but clean
    complete(success = false, reason)

  private def complete(success: Boolean, reason: String): Unit =
    self.setEndTime(new Date)
    self.setSuccess(success)
    self.setReason(reason)
end RunImpl
