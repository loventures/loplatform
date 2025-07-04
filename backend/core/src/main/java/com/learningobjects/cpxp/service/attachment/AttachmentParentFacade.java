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

import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.document.DocumentConstants;
import com.learningobjects.cpxp.service.query.Direction;

import java.util.List;

@FacadeItem("*")
public interface AttachmentParentFacade extends Facade {
    @FacadeChild(value = AttachmentConstants.ITEM_TYPE_ATTACHMENT, orderType = DocumentConstants.DATA_TYPE_CREATE_TIME, direction = Direction.DESC)
    public List<AttachmentFacade> getAttachments();
    public AttachmentFacade getAttachment(Long id);
    public void removeAttachment(Long id);
    public Long countAttachments();

    /**
     * Adds a child attachment.  NOTE: this method does not reside on loi.cp.attachment.AttachmentParentFacade to avoid polluting it with methods that use AttachmentFacade instead of AttachmentComponent
     * @param uploadInfo
     * @return
     */
    public AttachmentFacade addAttachment(UploadInfo uploadInfo);
}
