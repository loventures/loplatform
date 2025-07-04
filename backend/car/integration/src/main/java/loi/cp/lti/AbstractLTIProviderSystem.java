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

package loi.cp.lti;

import com.learningobjects.cpxp.component.annotation.Configuration;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.service.integration.IntegrationWebService;
import loi.cp.integration.AbstractSystem;
import loi.cp.integration.LtiSystemComponent;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractLTIProviderSystem<T extends LtiSystemComponent<T>> extends AbstractSystem<T>
        implements LtiSystemComponent<T> {
    protected static final String USERNAME_PARAMETER = "usernameParameter";

    @Inject
    private IntegrationWebService _integrationWebService;

    @Configuration(label = "$$label_key=Shared Secret Key", type = "Password", order = 11)
    public String getKey() {
        return (_self == null) ? null : _self.getKey();
    }

    public void setKey(String key) {
        _self.setKey(key);
    }

    @Override
    public T update(T system) {
        setUsernameParameter(system.getUsernameParameter());
        return super.update(system);
    }

    @PostCreate
    private void createSystem(LtiSystemComponent init) {
        _integrationWebService.addLTIRoleMappings(getId()); // 4realz?
    }

    @Override
    public String getOutcomeServiceUrl() {
        return _self.getCallbackPath();
    }

    @Override
    public void setOutcomeServiceUrl(String url) {
        _self.setCallbackPath(url);
    }

    @Override
    @Configuration(label = "$$label_customUsernameParameter=Custom Username Parameter", order = 10)
    public String getUsernameParameter() {
        return getStringConfigurationValue(USERNAME_PARAMETER);
    }

    @Override
    public void setUsernameParameter(String usernameParameter) {
        Map<String, Object> config = getConfigurationMap();
        config.put(USERNAME_PARAMETER, usernameParameter);
        setConfiguration(config);
    }

    @Override
    protected Map<String, Object> getConfigurationMap() {
        Map<String, Object> config = new HashMap<>();
        config.put(USERNAME_PARAMETER, getUsernameParameter());
        return config;
    }
}
