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

package com.learningobjects.cpxp.service.integration;

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.portal.NameFacade;

/**
 * A facade for external roles.
 */
@FacadeItem(IntegrationConstants.ITEM_TYPE_EXTERNAL_ROLE)
public interface ExternalRoleFacade extends NameFacade {
    @FacadeData(IntegrationConstants.DATA_TYPE_EXTERNAL_ROLE_ID)
    public String getRoleId();
    public void setRoleId(String roleId);

    @FacadeData(IntegrationConstants.DATA_TYPE_EXTERNAL_ROLE_TYPE)
    public String getRoleType();
    public void setRoleType(String roleType);
}

