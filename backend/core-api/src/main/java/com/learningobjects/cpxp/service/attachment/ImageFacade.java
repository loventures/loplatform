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

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.name.UrlFacade;

/**
 * A facade for attachments.
 */
@FacadeItem(AttachmentConstants.ITEM_TYPE_ATTACHMENT)
public interface ImageFacade extends UrlFacade {
    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME)
    String getFileName();
    void setFileName(String fileName);

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_SIZE)
    Long getSize();
    void setSize(Long size); // seminonpublic

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_THUMBNAIL)
    String getThumbnail();
    void setThumbnail(String thumbnail);

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_WIDTH)
    Long getWidth();
    void setWidth(Long width);

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_HEIGHT)
    Long getHeight();
    void setHeight(Long height);
}
