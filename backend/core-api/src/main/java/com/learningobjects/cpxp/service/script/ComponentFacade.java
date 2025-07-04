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

package com.learningobjects.cpxp.service.script;

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.portal.NameFacade;
import com.learningobjects.cpxp.service.site.SiteConstants;

@FacadeItem("*")
public interface ComponentFacade extends NameFacade {
    @FacadeData(DataTypes.DATA_TYPE_DISABLED)
    public Boolean getDisabled();
    public void setDisabled(Boolean disabled);

    @FacadeData(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER)
    public String getIdentifier();
    public void setIdentifier(String identifier);

    @FacadeData(ComponentConstants.DATA_TYPE_COMPONENT_CONFIGURATION)
    public String getConfiguration();
    public void setConfiguration(String configuration);

    @FacadeData(SiteConstants.DATA_TYPE_COMPONENT_CATEGORY)
    public String getCategory();
    public void setCategory(String category);
}
