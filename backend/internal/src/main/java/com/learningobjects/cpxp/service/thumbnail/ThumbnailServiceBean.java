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

package com.learningobjects.cpxp.service.thumbnail;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.FileInfo;
import com.learningobjects.cpxp.util.ImageUtils;
import com.typesafe.config.Config;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ThumbnailServiceBean extends BasicServiceBean implements ThumbnailService {
    private static final Logger logger = Logger.getLogger(ThumbnailServiceBean.class.getName());

    /** The attachment service. */
    @Inject
    private AttachmentService _attachmentService;

    /** The attachment Web service. */
    @Inject
    private AttachmentWebService _attachmentWebService;

    /** The data service. */
    @Inject
    private DataService _dataService;

    @Inject
    private Config config;

    private static final Pattern GEOMETRY_RE = Pattern.compile("((\\d+)\\+(\\d+)\\+(\\d+)@)?(\\d+)x(\\d+)");

    public void generateScaledImage(Item image) {

        AttachmentFacade scaled = _attachmentWebService.getRawAttachment(image.getId());
        AttachmentFacade parent = _attachmentWebService.getRawAttachment(image.getParent().getId());
        FileInfo imageBlob = _attachmentService.getAttachmentBlob(image.getParent());
        boolean hasImage = (scaled != null) && (scaled.getDigest() != null) && !AttachmentConstants.ATTACHMENT_DIGEST_BROKEN.equals(scaled.getDigest());

        if ((scaled == null) || (parent == null) || (parent.getWidth() == null) || (imageBlob == null) || hasImage) {
            return;
        }

        try {
            String fileName = parent.getFileName();
            int index = fileName.lastIndexOf('.');
            String suffix = fileName.substring(index);
            File thumbFile = File.createTempFile("Attachment", suffix);
            try {
                thumbFile.deleteOnExit();

                Thumbnailer thumbnailer = getThumbnailer();
                thumbnailer.setSource(imageBlob);
                thumbnailer.setDestination(thumbFile);

                String geometry = scaled.getGeometry();
                Matcher matcher = GEOMETRY_RE.matcher(scaled.getGeometry());
                if (!matcher.matches()) {
                    throw new RuntimeException("Invalid geometry: " + geometry);
                }

                if (matcher.group(1) != null) {
                    int size = Integer.parseInt(matcher.group(2));
                    int x = Integer.parseInt(matcher.group(3));
                    int y = Integer.parseInt(matcher.group(4));
                    thumbnailer.setWindow(x, y, size, size);
                    thumbnailer.setThumbnail(true);
                    // hack.. assume if there's a window i want to generate a thumbnail
                    // which means also strip metadata etc. from the image.
                }
                int width = Integer.parseInt(matcher.group(5));
                int height = Integer.parseInt(matcher.group(6));
                thumbnailer.setDimensions(width, height, true);
                thumbnailer.setQuality((width * height <= 4096) ? 0.9f : 0.85f);
                thumbnailer.thumbnail();

                ImageUtils.Dim dim = ImageUtils.getImageDimensions(thumbFile);

                scaled.setFileName(fileName.substring(0, index) + "_" + width + "x" + height + suffix);
                scaled.setWidth((long) dim.getWidth());
                scaled.setHeight((long) dim.getHeight());
                scaled.setDisposition(parent.getDisposition());
                scaled.setGeometry(geometry);

                _attachmentWebService.updateAttachment(scaled.getId(), thumbFile);
            } finally {
                thumbFile.delete();
            }
        } catch (Throwable th) {
            logger.log(Level.WARNING, "Thumbnail generation failed: " + parent + " @ " + scaled.getGeometry(), th);
            _dataService.setString(image,AttachmentConstants.DATA_TYPE_ATTACHMENT_DIGEST, AttachmentConstants.ATTACHMENT_DIGEST_BROKEN);
        }

    }

    private Thumbnailer getThumbnailer() {
        Config thumbnailConfig = config.getConfig("com.learningobjects.cpxp.thumbnail");
        String provider  = thumbnailConfig.getString("provider");
        return ThumbnailerFactory.getThumbnailer(provider);
    }
}
