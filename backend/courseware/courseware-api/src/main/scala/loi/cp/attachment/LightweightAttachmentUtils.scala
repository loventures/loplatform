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

package loi.cp.attachment

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.web.{ErrorResponse, FileResponse}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.attachment.{AttachmentFacade, AttachmentWebService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.folder.FolderFacade
import loi.cp.admin.FolderParentFacade
import loi.cp.security.SecuritySettings
import scalaz.\/
import scalaz.syntax.std.option.*
import scalaz.syntax.std.tuple.*
import scalaz.syntax.zip.*
import scaloi.syntax.`class`.*
import scaloi.syntax.boolean.*

/** A set of utilities to create and load attachments. Full services for serving attachment requests without direct use
  * of facades and components can be built upon these services.
  */
object LightweightAttachmentUtils:

  /** Finds the folder for the attachments.
    *
    * @param attachmentFolderParentId
    *   the id of the item that should contain the attachment folder
    * @param folderType
    *   the type of folder (generally to distinguish one attachment type from another)
    * @param attachmentWebService
    *   the attachment service to drive these methods
    * @param facadeService
    *   the facade service
    * @return
    *   a folder containing the attachments
    */
  def getOrCreateFolder(
    attachmentFolderParentId: Id,
    folderType: String
  )(implicit attachmentWebService: AttachmentWebService, facadeService: FacadeService): FolderFacade =
    val folderParent = attachmentFolderParentId.facade[FolderParentFacade]
    folderParent.getOrCreateFolderByType(folderType).result

  /** Adds a new attachments to the attachment folder.
    *
    * @param attachmentFolderParentId
    *   the id of the item that should contain the attachment folder
    * @param folderType
    *   the type of folder (generally to distinguish one attachment type from another)
    * @param reference
    *   a reference to the associated content (post, attempt, ...)
    * @param uploadInfo
    *   the data to create the attachment from
    * @param attachmentWebService
    *   the attachment service to drive these methods
    * @param facadeService
    *   the facade service
    * @return
    */
  def addAttachment(attachmentFolderParentId: Id, folderType: String, reference: Long)(
    uploadInfo: UploadInfo
  )(implicit attachmentWebService: AttachmentWebService, facadeService: FacadeService): AttachmentId =
    val folder     = getOrCreateFolder(attachmentFolderParentId, folderType)
    val attachment = attachmentWebService.createAttachment(folder.getId, uploadInfo)
    attachment.facade[AttachmentFacade].setReference(reference)
    AttachmentId(attachment)

  import scaloi.syntax.boxes.*

  /** Returns an attachment specified by an id as a [[FileResponse]].
    *
    * @param attachmentFolderParentId
    *   the id of the item that should contain the attachment folder
    * @param folderType
    *   the type of folder (generally to distinguish one attachment type from another)
    * @param reference
    *   an optional reference item that the attachment must match
    * @param attachmentId
    *   the id of the attachment to fetch
    * @param download
    *   use Content-Disposition header to force a download rather than inline display
    * @param direct
    *   force direct return of the response; inhibits use of a CDN
    * @param size
    *   a logical thumbnail size - small, medium, large, xlarge
    * @param security
    *   the domain security settings
    * @param attachmentWebService
    *   the attachment service to drive these methods
    * @param facadeService
    *   the facade service
    * @return
    *   the [[FileResponse]] for the attachment, or an [[ErrorResponse]] if it fails
    */
  def buildFileResponse(
    attachmentFolderParentId: Id,
    folderType: String,
    reference: Option[Long],
    security: SecuritySettings
  )(attachmentId: AttachmentId, download: Boolean, direct: Boolean, size: String)(implicit
    attachmentWebService: AttachmentWebService,
    facadeService: FacadeService,
    cs: ComponentService
  ): ErrorResponse \/ FileResponse.Any =
    val folder: FolderFacade = getOrCreateFolder(attachmentFolderParentId, folderType)
    val attachmentOpt        = attachmentId.value
      .component_?[AttachmentComponent]
      .filter(_.getParentId == folder.getId) // Only allow requests for attachments in the folder
      .filter(_.getReference.unboxInsideTo[Option]() `doesNotDisagreeWith` reference)

    for
      attachment <- attachmentOpt \/> ErrorResponse.notFound
      _          <- SecuritySettings.isAllowedFilename(attachment.getFileName, security) \/> ErrorResponse.forbidden
      // there is no case where it would make sense for the server to cache any of these attachments
      // (discussions, assessments) for "quicker" direct download
      response    = attachment.viewInternal(download, direct, size, false, true)
      _          <- classOf[ErrorResponse].option(response) <\/ (())
      file       <- classOf[FileResponse.Any].option(response) \/> ErrorResponse.serverError("unexpected response")
    yield file
  end buildFileResponse

  /** Returns the attachments specified by given ids as a [[AttachmentInfo]].
    *
    * @param attachmentFolderParentId
    *   the id of the item that should contain the attachment folder
    * @param folderType
    *   the type of folder (generally to distinguish one attachment type from another)
    * @param attachmentIds
    *   the ids of the attachments to fetch
    * @param attachmentWebService
    *   the attachment service to drive these methods
    * @param facadeService
    *   the facade service
    * @return
    *   a collection of [[AttachmentInfo]] s
    */
  def loadAttachmentInfos(attachmentFolderParentId: Id, folderType: String)(attachmentIds: Seq[AttachmentId])(implicit
    attachmentWebService: AttachmentWebService,
    facadeService: FacadeService,
    cs: ComponentService
  ): Seq[AttachmentInfo] =
    val folder: FolderFacade = getOrCreateFolder(attachmentFolderParentId, folderType)

    attachmentIds
      .map(_.value)
      .components[AttachmentComponent]
      .filter(_.getParentId == folder.getId) // Only allow requests for attachments in the folder
      .map(AttachmentInfo(_))
  end loadAttachmentInfos

  private implicit final class LocalOptionOps[A](private val fa: Option[A]) extends AnyVal:

    /** Returns true if either option is absent or both are present and equal. */
    def doesNotDisagreeWith(fb: Option[A]): Boolean = fa.fzip(fb).forall(_.biequal)

  private implicit final class LocalTupleOps[A](private val ta: (A, A)) extends AnyVal:

    /** Returns true if both elements in the tuple are equal. */
    def biequal: Boolean = ta.fold(_ == _)
end LightweightAttachmentUtils
