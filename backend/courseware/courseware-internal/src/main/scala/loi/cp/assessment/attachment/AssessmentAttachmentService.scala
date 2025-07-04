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

package loi.cp.assessment.attachment

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.{ErrorResponse, FileResponse}
import com.learningobjects.cpxp.controller.upload.{UploadInfo, UploadedFile, Uploads}
import com.learningobjects.cpxp.service.attachment.{AttachmentWebService, Disposition}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.assessment.AttemptId
import loi.cp.attachment.{AttachmentId, AttachmentInfo, LightweightAttachmentUtils}
import loi.cp.config.ConfigurationService
import loi.cp.security.SecuritySettings
import scalaz.\/
import scalaz.syntax.std.option.*

import scala.concurrent.duration.*

@Service
trait AssessmentAttachmentService:
  def addAttachment(userId: UserId, attemptId: AttemptId, uploadInfo: UploadInfo): AttachmentId

  def getUpload(uploadedFile: UploadedFile): UploadInfo

  def buildFileResponse(
    userId: UserId,
    attemptId: AttemptId,
    attachmentId: AttachmentId,
    download: Boolean,
    direct: Boolean,
    size: String = null
  ): ErrorResponse \/ FileResponse.Any

  def buildRedirectUrl(
    userId: UserId,
    attemptId: AttemptId,
    attachmentId: AttachmentId,
  ): ErrorResponse \/ String

  def loadAttachmentInfos(userId: UserId, ids: Seq[AttachmentId]): Seq[AttachmentInfo]
end AssessmentAttachmentService

abstract class AssessmentAttachmentServiceImpl(implicit
  attachmentWebService: AttachmentWebService,
  facadeService: FacadeService,
  cs: ComponentService,
  cfgs: ConfigurationService
) extends AssessmentAttachmentService:
  val assessmentAttachmentsType: String

  override def addAttachment(userId: UserId, attemptId: AttemptId, uploadInfo: UploadInfo): AttachmentId =
    LightweightAttachmentUtils.addAttachment(userId, assessmentAttachmentsType, attemptId.value)(uploadInfo)

  override def getUpload(uploadedFile: UploadedFile): UploadInfo =
    Uploads.retrieveUpload(uploadedFile.guid)

  override def buildFileResponse(
    userId: UserId,
    attemptId: AttemptId,
    attachmentId: AttachmentId,
    download: Boolean,
    direct: Boolean,
    size: String
  ): ErrorResponse \/ FileResponse.Any =
    LightweightAttachmentUtils.buildFileResponse(
      userId,
      assessmentAttachmentsType,
      Some(attemptId.value),
      SecuritySettings.config.getDomain
    )(attachmentId, download, direct, size)

  // This needs the file in S3 (i.e. "noDirect") so it can get a public signed S3 URL to hand off to MS Office
  override def buildRedirectUrl(
    userId: UserId,
    attemptId: AttemptId,
    attachmentId: AttachmentId,
  ): ErrorResponse \/ String =
    for
      fileResponse <- LightweightAttachmentUtils.buildFileResponse(
                        userId,
                        assessmentAttachmentsType,
                        Some(attemptId.value),
                        SecuritySettings.config.getDomain
                      )(attachmentId, download = false, direct = false, size = null)
      // Office previewer suddenly can't handle any unusual characters in the filename
      _             = fileResponse.fileInfo.setDisposition(Disposition.attachment, s"${attemptId.value}.docx")
      url          <- Option(fileResponse.fileInfo.getDirectUrl("GET", 1.hour.toMillis)) \/> ErrorResponse.badRequest
    yield url

  override def loadAttachmentInfos(userId: UserId, attachmentIds: Seq[AttachmentId]): Seq[AttachmentInfo] =
    LightweightAttachmentUtils.loadAttachmentInfos(userId, assessmentAttachmentsType)(attachmentIds)

  private val log = org.log4s.getLogger
end AssessmentAttachmentServiceImpl
