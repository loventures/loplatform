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

package loi.cp.accesscode;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.component.misc.AccessCodeConstants;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.user.UserFacade;

import java.util.Date;

@FacadeItem(AccessCodeConstants.ITEM_TYPE_ACCESS_CODE_BATCH)
public interface AccessCodeBatchFacade extends Facade {
    @FacadeData
    public String getName();
    public void setName(String name);

    @FacadeData
    public UserFacade getCreator();
    public void setCreator(Id creator);

    @FacadeData
    public Date getCreateTime();
    public void setCreateTime(Date createTime);

    @FacadeData
    public Boolean getDisabled();
    public void setDisabled(Boolean disabled);

    @FacadeData
    public Long getItem();
    public void setItem(Long item);

    @FacadeJson
    public void setAttribute(String name, Object value);
    public <T> T getAttribute(String name, Class<T> type);

    @FacadeJson
    public Long getQuantity();
    public void setQuantity(Long quantity);

    @FacadeJson
    public String getDuration();
    public void setDuration(String duration);

    @FacadeJson
    public Long getRedemptionLimit();
    public void setRedemptionLimit(Long redemptionLimit);

    @FacadeChild(AttachmentConstants.ITEM_TYPE_ATTACHMENT)
    public AttachmentFacade getImport();
    public void addImport(UploadInfo upload);

    @FacadeParent
    public AccessCodeParentFacade getParent();

    @FacadeChild(AccessCodeConstants.ITEM_TYPE_REDEMPTION)
    @FacadeQuery(domain = true)
    QueryBuilder queryAllRedemptions();

}
