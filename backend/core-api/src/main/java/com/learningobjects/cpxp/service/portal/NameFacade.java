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

package com.learningobjects.cpxp.service.portal;

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.group.GroupFacade;
import com.learningobjects.cpxp.service.user.UserFacade;

/**
 * A facade used to render the name of something.
 */
@FacadeItem("*")
public interface NameFacade extends UserFacade, GroupFacade {
    @FacadeData(DataTypes.DATA_TYPE_NAME)
    public String getName();
    public void setName(String name);

    @FacadeData(DataTypes.DATA_TYPE_MSG)
    public String getMsg();
    public void setMsg(String msg);

//    @FacadeData(type = DataTypes.DATA_TYPE_LOGO)
//    public AttachmentFacade getLogo();

    @FacadeData(AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME)
    public String getFileName();
}
