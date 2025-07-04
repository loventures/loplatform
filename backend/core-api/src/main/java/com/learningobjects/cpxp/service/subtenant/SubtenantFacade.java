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

package com.learningobjects.cpxp.service.subtenant;

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeComponent;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.data.DataTypes;


@FacadeItem(SubtenantConstants.ITEM_TYPE_SUBTENANT)
public interface SubtenantFacade extends Facade {
    @FacadeData(SubtenantConstants.DATA_TYPE_TENANT_ID)
    String getTenantId();
    void setTenantId(String id);

    @FacadeData(SubtenantConstants.DATA_TYPE_SUBTENANT_NAME)
    String getName();
    void setName(String name);

    @FacadeData(SubtenantConstants.DATA_TYPE_SUBTENANT_SHORT_NAME)
    String getShortName();
    void setShortName(String name);

    @FacadeData(SubtenantConstants.DATA_TYPE_SUBTENANT_LOGO)
    AttachmentFacade getLogo();
    void setLogo(Long logo);

    void delete();
}
