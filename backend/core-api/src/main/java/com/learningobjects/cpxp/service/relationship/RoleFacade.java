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

package com.learningobjects.cpxp.service.relationship;

import com.google.common.base.Function;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.dto.FacadeParent;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.portal.NameFacade;

import javax.annotation.Nullable;

/**
 * A facade for roles.
 */
@FacadeItem(RelationshipConstants.ITEM_TYPE_ROLE)
public interface RoleFacade extends NameFacade {
    @FacadeParent
    public Long getParentId();

    @FacadeData(RelationshipConstants.DATA_TYPE_ROLE_ID)
    public String getRoleId();
    public void setRoleId(String roleId);

    @FacadeData(DataTypes.DATA_TYPE_ID)
    public String getIdStr();
    public void setIdStr(String idStr);
}
