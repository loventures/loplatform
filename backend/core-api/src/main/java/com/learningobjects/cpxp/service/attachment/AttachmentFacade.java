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

import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeCondition;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.document.DocumentConstants;
import com.learningobjects.cpxp.service.user.UserFacade;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * A facade for attachments.
 */
@FacadeItem(AttachmentConstants.ITEM_TYPE_ATTACHMENT)
public interface AttachmentFacade extends ImageFacade {
    @FacadeData(DocumentConstants.DATA_TYPE_CREATE_TIME)
    Date getCreated();
    void setCreated(Date created);

    @FacadeData(value = DocumentConstants.DATA_TYPE_CREATOR)
    UserFacade getCreator();
    void setCreator(Long creator);

    @FacadeData(DocumentConstants.DATA_TYPE_EDIT_TIME)
    Date getEdited();
    void setEdited(Date edited);

    @FacadeData(value = DocumentConstants.DATA_TYPE_EDITOR)
    UserFacade getEditor();
    void setEditor(Long editor);

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_CLIENT_ID)
    String getClientId();
    void setClientId(String clientId);

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_DISPOSITION)
    Disposition getDisposition();
    void setDisposition(Disposition disp);

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_DIGEST)
    String getDigest();
    void setDigest(String digest); // seminonpublic

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_PROVIDER)
    String getProvider();
    void setProvider(String provider); // seminonpublic

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_GEOMETRY)
    String getGeometry();
    void setGeometry(String geometry); // seminonpublic

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_ACCESS)
    Optional<AttachmentAccess> getAccess();
    void setAccess(AttachmentAccess access);

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_GENERATION)
    Long getGeneration();
    void setGeneration(Long generation);

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_REFERENCE)
    Optional<Long> getReference();
    void setReference(Long reference);

    /**
     * Child attachments are used by:
     * - CampusPack to save variations of the main image, e.g. cropped profile pictures, scaled thumbnails, etc.
     * - Lumen to save multiple attachments under a single EssayResponse.
     */
    @FacadeChild(AttachmentConstants.ITEM_TYPE_ATTACHMENT)
    List<AttachmentFacade> getAttachments();
    AttachmentFacade findAttachmentByGeometry(
      @FacadeCondition(AttachmentConstants.DATA_TYPE_ATTACHMENT_GEOMETRY) String geometry
    );



    void lock(boolean pessimistic);
}
