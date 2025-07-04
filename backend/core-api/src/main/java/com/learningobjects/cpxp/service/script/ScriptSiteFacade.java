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

import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeCondition;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.name.UrlFacade;
import com.learningobjects.cpxp.service.site.SiteConstants;

import java.util.List;

@FacadeItem(SiteConstants.ITEM_TYPE_SITE)
public interface ScriptSiteFacade extends UrlFacade {
    @FacadeData(SiteConstants.DATA_TYPE_COMPONENT_GENERATION)
    Long getGeneration();
    void setGeneration(Long generation);

    @FacadeChild(ScriptConstants.ITEM_TYPE_SCRIPT_ARCHIVE)
    List<ComponentArchiveFacade> getComponentArchives();
    ComponentArchiveFacade addComponentArchive();
    void removeComponentArchive(ComponentArchiveFacade archive);
    ComponentArchiveFacade findComponentArchiveByIdentifier(
      @FacadeCondition(ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_IDENTIFIER)
      String identifier
    );

    @FacadeData(ScriptConstants.DATA_TYPE_ADMIN_COMPONENT_AVAILABILITY)
    String getEnabledMap();
    void setEnabledMap(String availability);

    @FacadeData(ScriptConstants.DATA_TYPE_STATIC_COMPONENT_CONFIGURATIONS)
    String getConfigurationMap();
    void setConfigurationMap(String staticConfigurations);

    void refresh(boolean pessimistic);
}
