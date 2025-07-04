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

package loi.cp.asset.base.service;

import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.controller.upload.Uploader;
import com.learningobjects.cpxp.filter.SendFileFilter;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.mime.MimeWebService;
import com.learningobjects.cpxp.util.FileInfo;
import com.learningobjects.cpxp.util.HttpUtils;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Helper for file uploads and file downloads.
 */
@Service
public class AttachmentService {
    @Inject
    private AttachmentWebService _attachmentWebService;

    @Inject
    private MimeWebService _mimeWebService;

    /**
     * Takes HTTP requests with a file as request body and spits out an UploadInfo.
     * Singlepart and multipart are supported. Just the first file of a multipart
     * upload is returned.
     *
     * @param request Request containing the upload
     */
    public UploadInfo receive(HttpServletRequest request) {
        return receive(request, RequestBody.SINGLE_PART_NAME);
    }

    /**
     * Takes HTTP requests with a file as request body and spits out an UploadInfo.
     * Singlepart and multipart are supported. If no part is named, just the first
     * file of a multipart upload is returned.
     *
     * @param request Request containing the upload
     * @param part The part name, or empty
     */
    public UploadInfo receive(HttpServletRequest request, String part) {
        UploadInfo uploadInfo = null;
        try {
            Uploader uploader = Uploader.parse(request);
            if ((part == null) || part.isEmpty()) {
                uploadInfo = uploader.getUploads().iterator().next();
            } else {
                uploadInfo = uploader.getUpload(part);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Upload Error", ex);
        }
        return uploadInfo;
    }

    /**
     * Cause a file to be sent in response.
     *
     * @param attachment Attachment of file to send
     * @param request
     */
    public void triggerDownload(AttachmentFacade attachment, HttpServletRequest request) {
        FileInfo info = _attachmentWebService.getAttachmentBlob(attachment.getId());
        info.setLastModified(attachment.getCreated());
        info.setContentType(_mimeWebService.getMimeType(attachment.getFileName()));
        info.setDoCache(false);
        info.setDisposition(HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, attachment.getFileName()));

        /*
         * com.learningobjects.cpxp.filter.SendFileFilter will see this and stream the file out
         */
        request.setAttribute(SendFileFilter.REQUEST_ATTRIBUTE_SEND_FILE, info);
    }
}
