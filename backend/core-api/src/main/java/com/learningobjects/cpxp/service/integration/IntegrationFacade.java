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

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.dto.FacadeParent;
import com.learningobjects.cpxp.service.portal.NameFacade;

@FacadeItem(IntegrationConstants.ITEM_TYPE_INTEGRATION)
public interface IntegrationFacade extends Facade {

    @FacadeData(IntegrationConstants.DATA_TYPE_UNIQUE_ID)
    public String getUniqueId();
    public void setUniqueId(String uniqueId);

    @FacadeData(IntegrationConstants.DATA_TYPE_DATA_SOURCE)
    public String getDataSource();
    public void setDataSource(String dataSource);

    @FacadeData(IntegrationConstants.DATA_TYPE_EXTERNAL_SYSTEM)
    public Long getExternalSystem();
    public void setExternalSystem(Long externalSystem);

    @FacadeData(IntegrationConstants.DATA_TYPE_EXTERNAL_SYSTEM)
    public SystemFacade getExternalSystemFacade();

    @FacadeParent
    public NameFacade getParent();
}
