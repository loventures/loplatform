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

package com.learningobjects.cpxp.service.attachment;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.BlobInfo;
import com.learningobjects.cpxp.util.FileInfo;

import javax.ejb.Local;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The attachment web service.
 */
@Local
public interface AttachmentWebService {
    public AttachmentFacade getAttachment(Long id);

    public AttachmentFacade getAttachment(Item item);

    public AttachmentFacade getRawAttachment(Long id);

    /** Gets a version of this attachment constrained to a given maximum window. */
    public AttachmentFacade getScaledImage(Long id, int maxWidth, int maxHeight, boolean generate);

    public Long getScaledImage(Long id, String geometry);

    public void setThumbnailGeometry(Long id, String thumbnail);

    public AttachmentFacade addAttachment(Long parentId, File file);

    public Long createAttachment(Long parentId, UploadInfo upload);

    public Long createPublicAttachment(Long parentId, UploadInfo upload);

    public void updateAttachment(Long id, File file);

    public void destroyAttachment(Long id);

    public AttachmentFacade createPlaceholder(Id parentId);

    /**
     * Copies the attachment of {@code attachmentId} into a new attachment under
     * the item of {@code parentId}. Sets a new URL if the parent has a URL.
     *
     * @return the new attachment
     */
    AttachmentFacade copyAttachment(final Long attachmentId, final Long parentId);

    /**
     * Copies the attachment of {@code attachmentId} into a new attachment under
     * the item of {@code parentId}.
     *
     * <ul>
     *   <li>Does not set a new URL on any target attachments even if the
     *   destination parent has a URL.</li>
     *   <li>Does not copy descendants that are leaf entities.</li>
     * </ul>
     *
     * You should only use this method if the above issues are relevant to
     * {@code attachmentId}.
     *
     */
    // the above issues were irrelevant to the > 62,000 attachments we had to
    // copy for heavyweight migration, so they are probably irrelevant to all
    // attachments. YMMV. The destination parent didn't have a URL. The only
    // descendants were immediate child attachments (which are peered).
    AttachmentFacade copyAttachmentUnsafely(final Long attachmentId, final Long parentId);

    public BlobInfo getAttachmentBlob(Long id);

    public FileInfo getCachedAttachmentBlob(Long id);

    public void removeImage(Long id, String type);

    public List<AttachmentFacade> getAttachments(Item item);

    public File getZipAttachmentFile(Long attachmentId, String pathInZip) throws IOException;

    public File getZipBlobFile(BlobInfo blobInfo, String pathInZip) throws IOException;

    public Map<String, File> getZipAttachmentFiles(Long attachmentId) throws IOException;

    /** Copy the source image to the specified parent and sets the data type if non null. */
    public ImageFacade copyImage(Long parentId, String dataType, ImageFacade image);
}
