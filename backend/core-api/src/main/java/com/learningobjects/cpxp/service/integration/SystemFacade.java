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
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.data.DataTypes;

import java.util.List;

@FacadeItem(IntegrationConstants.ITEM_TYPE_SYSTEM)
public interface SystemFacade extends Facade {
    // The hodge podge of entity mapped, data mapped, component config mapped and json mapped
    // is offensive. Don't add any more things that aren't json.

    @FacadeData(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER)
    public String getIdentifier();
    public void setIdentifier(String identifier);

    @FacadeData(ComponentConstants.DATA_TYPE_COMPONENT_CONFIGURATION)
    public String getConfiguration();
    public void setConfiguration(String configuration);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_ID)
    public String getSystemId();
    public void setSystemId(String id);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_NAME)
    public String getName();
    public void setName(String name);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_ALLOW_LOGIN)
    public Boolean getAllowLogin();
    public void setAllowLogin(Boolean allowLogin);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_URL)
    public String getUrl();
    public void setUrl(String url);

    @FacadeData(IntegrationConstants.DATA_TYPE_CALLBACK_PATH)
    public String getCallbackPath();
    public void setCallbackPath(String url);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_LOGIN)
    public String getLogin();
    public void setLogin(String login);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_PASSWORD)
    public String getPassword();
    public void setPassword(String password);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_KEY)
    public String getKey();
    public void setKey(String key);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_USE_EXTERNAL_IDENTIFIER)
    public Boolean getUseExternalIdentifier();
    public void setUseExternalIdentifier(Boolean useExternalIdentifier);

    @FacadeData(IntegrationConstants.DATA_TYPE_SYSTEM_RIGHTS)
    public String getRights();
    public void setRights(String rights);

    @FacadeData(DataTypes.DATA_TYPE_DISABLED)
    public Boolean getDisabled();
    public void setDisabled(Boolean disabled);

    @FacadeData(DataTypes.DATA_TYPE_JSON)
    <T> T getJsonConfig(Class<T> type);
    void setJsonConfig(Object value);

    @FacadeChild(IntegrationConstants.ITEM_TYPE_EXTERNAL_ROLE)
    public List<ExternalRoleFacade> getExternalRoles();

    public ExternalRoleFacade getExternalRole(Long id);
    public ExternalRoleFacade addExternalRole();
    public void removeExternalRole(Long id);

    @FacadeChild(IntegrationConstants.ITEM_TYPE_ROLE_MAPPING)
    public List<RoleMappingFacade> getRoleMappings();

    public RoleMappingFacade getRoleMapping(Long id);
    public RoleMappingFacade addRoleMapping();
    public void removeRoleMapping(Long id);
}
