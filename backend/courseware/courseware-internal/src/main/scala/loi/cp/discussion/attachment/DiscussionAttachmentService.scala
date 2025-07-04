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

package loi.cp.discussion.attachment

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.{ErrorResponse, FileResponse}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.attachment.{AttachmentId, AttachmentInfo, LightweightAttachmentUtils}
import loi.cp.config.ConfigurationService
import loi.cp.discussion.PostId
import loi.cp.reference.ContentIdentifier
import loi.cp.security.SecuritySettings
import scalaz.\/

/** A service for adding and fetching attachments to a discussion
  */
@Service
trait DiscussionAttachmentService:
  def addAttachment(discussionContentId: ContentIdentifier, postId: PostId, uploadInfo: UploadInfo): AttachmentId

  def buildFileResponse(
    discussionContentId: ContentIdentifier,
    postId: PostId,
    attachmentId: AttachmentId,
    download: Boolean,
    direct: Boolean,
    size: String = null
  ): ErrorResponse \/ FileResponse.Any

  def loadAttachmentInfos(discussionContentId: ContentIdentifier, ids: Seq[AttachmentId]): Seq[AttachmentInfo]
end DiscussionAttachmentService

@Service
class DiscussionAttachmentServiceImpl(implicit
  attachmentWebService: AttachmentWebService,
  facadeService: FacadeService,
  cs: ComponentService,
  cfgs: ConfigurationService
) extends DiscussionAttachmentService:
  import DiscussionAttachmentService.*

  override def addAttachment(
    discussionContentId: ContentIdentifier,
    postId: PostId,
    uploadInfo: UploadInfo
  ): AttachmentId =
    LightweightAttachmentUtils.addAttachment(discussionContentId.contextId, DiscussionAttachmentsType, postId)(
      uploadInfo
    )

  override def buildFileResponse(
    discussionContentId: ContentIdentifier,
    postId: PostId,
    attachmentId: AttachmentId,
    download: Boolean,
    direct: Boolean,
    size: String
  ): ErrorResponse \/ FileResponse.Any =
    LightweightAttachmentUtils.buildFileResponse(
      discussionContentId.contextId,
      DiscussionAttachmentsType,
      Some(postId),
      SecuritySettings.config.getDomain
    )(attachmentId, download, direct, size)

  override def loadAttachmentInfos(
    discussionContentId: ContentIdentifier,
    ids: Seq[AttachmentId]
  ): Seq[AttachmentInfo] =
    LightweightAttachmentUtils.loadAttachmentInfos(discussionContentId.contextId, DiscussionAttachmentsType)(ids)
end DiscussionAttachmentServiceImpl

object DiscussionAttachmentService:
  val DiscussionAttachmentsType = "discussion-attachments"
