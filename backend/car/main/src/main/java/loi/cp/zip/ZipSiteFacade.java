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

package loi.cp.zip;

import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.dto.FacadeJson;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.document.DocumentConstants;
import com.learningobjects.cpxp.service.query.Direction;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.site.SiteConstants;
import com.learningobjects.cpxp.service.site.SiteFacade;

import java.util.List;

@FacadeItem(SiteConstants.ITEM_TYPE_SITE)
public interface ZipSiteFacade extends SiteFacade {
    @FacadeData(DataTypes.DATA_TYPE_ATTACHMENT)
    public AttachmentFacade getActiveAttachment();
    public void setActiveAttachment(Long id);

    @FacadeChild(value = AttachmentConstants.ITEM_TYPE_ATTACHMENT, orderType = DocumentConstants.DATA_TYPE_CREATE_TIME, direction = Direction.DESC)
    public List<AttachmentFacade> getAttachments();
    public QueryBuilder queryAttachments();
    public AttachmentFacade getAttachment(Long id);
    public void removeAttachment(Long id);
    public AttachmentFacade addAttachment(UploadInfo info);

    @FacadeJson
    String getViewRightClassName();
    void setViewRightClassName(String viewRightClassName);

}
