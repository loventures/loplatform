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

package loi.cp.integration;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.component.util.ConfigUtils;
import com.learningobjects.cpxp.service.integration.SystemFacade;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSystem<A extends SystemComponent<A>> extends AbstractComponent implements SystemComponent<A> {
    @Instance
    protected SystemFacade _self;

    @PostCreate
    private void initAbstractSystem(A init) {
        _self.setAllowLogin(this instanceof LoginSystemComponent);
        if (init != null) {
            update(init);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public A update(A system) {
        // TODO: systemId clash!?!
        _self.setSystemId(system.getSystemId());
        _self.setName(system.getName());
        _self.setKey(system.getKey());
        _self.setDisabled(Boolean.TRUE.equals(system.getDisabled()));
        _self.invalidateParent();
        return (A) this; // in scala, self: A =>
    }

    @Override
    public void delete() {
        _self.delete();
        _self.invalidateParent();
    }

    @Override
    public Long getId() {
        return (_self == null) ? null : _self.getId();
    }

    @Override
    public String getSystemId() {
        return (_self == null) ? null : _self.getSystemId();
    }

    @Override
    public String getName() {
        return (_self == null) ? null : _self.getName();
    }

    @Override
    public String getKey() {
        return _self.getKey();
    }

    @Override
    public void setDisabled(Boolean disabled) {
        _self.setDisabled(disabled);
    }

    @Override
    public Boolean getDisabled() {
        return _self.getDisabled();
    }

    @Override
    public String getImplementation() {
        return getComponentInstance().getIdentifier();
    }

    protected String getStringConfigurationValue(String name) {
        if (_self == null) return null;
        String[] config = ConfigUtils.decodeConfiguration(_self.getConfiguration()).get(name);
        return (String) ConfigUtils.decodeValues(config, String.class);
    }

    public void setConfiguration(Map<String, ?> config) {
        _self.setConfiguration(ConfigUtils.encodeConfiguration(config));
    }

    protected SystemFacade facade() {
        return _self;
    }

    /**
     * Sets a single string configuration value. Retains all other configuration values.
     */
    protected void setStringConfiguration(String name, String value) {
        Map<String, Object> config = getConfigurationMap();
        config.put(name, value);
        setConfiguration(config);
    }

    /**
     * Gets a map of all configuration values. Subclasses with additional configuration values should override this method.
     */
    protected Map<String, Object> getConfigurationMap() {
        return new HashMap<>();
    }
}
