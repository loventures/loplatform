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

package loi.cp.blob.attachment;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.Projection;
import com.learningobjects.cpxp.service.query.QueryService;
import loi.cp.integration.AbstractSystem;
import scala.Function1;
import scala.Function2;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;

import javax.inject.Inject;
import java.io.*;
import java.util.logging.Logger;

/** A blob storage system mock that simply delegates to AttachmentService. For dev use only */
@Component(name = "Attachment Blob Storage")
public class AttachmentBlobStorageSystem extends AbstractSystem<AttachmentBlobStorageSystemComponent> implements AttachmentBlobStorageSystemComponent {
    private static final Logger logger = Logger.getLogger(AttachmentBlobStorageSystem.class.getName());

    @Inject
    AttachmentWebService _attachmentWebService;

    @Inject
    QueryService _queryService;

    @Override
    public <R> Either<IOException, R> writeTo(String blobName, Function2<UploadInfo, R, R> uiTx, Function1<OutputStream, R> cont) {
        try {
            File temp = File.createTempFile("localblob-", ".csv");
            logger.info("Trying to write blob "+blobName+" to attachment bucket");
            try (OutputStream os = new FileOutputStream(temp)) {
                R res = cont.apply(os);
                UploadInfo att = new UploadInfo(blobName, "application/octet-stream", temp, true);
                Long aid = _attachmentWebService.createAttachment(_self.getId(), att);
                logger.info("Wrote blob "+blobName+" as [Attachment/" + aid + "]");
                return Right.apply(uiTx.apply(att, res));
            } catch (IOException ex) {
                logger.severe(String.format("Failed to open or write to file %s (%s)", temp.getAbsolutePath(), ex.getClass().getName()));
                return Left.apply(ex);
            }
        } catch (IOException ex) {
            logger.severe("Cannot create temp file: " + ex.getMessage());
            return Left.apply(ex);
        }
    }

    @Override
    public <T> Either<IOException, T> readFrom(String blobName, Function1<InputStream, T> cont) {
        Long attachmentId = _queryService.queryParent(_self.getId(), AttachmentConstants.ITEM_TYPE_ATTACHMENT)
          .addCondition(AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME, Comparison.eq, blobName)
          .setProjection(Projection.ID)
          .getResult();
        try (InputStream is = _attachmentWebService.getAttachmentBlob(attachmentId).openInputStream()) {
            return Right.apply(cont.apply(is));
        } catch (IOException ex) {
            logger.severe(String.format("Failed to open or read from file %s (%s)", blobName, ex.getClass().getName()));
            return Left.apply(ex);
        }
    }
}
