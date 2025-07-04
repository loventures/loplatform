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

package loi.cp.canvas;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Configuration;
import loi.cp.lti.AbstractLTIProviderSystem;

import java.util.Map;

@Component(name = "$$name=Canvas")
public class CanvasSystem extends AbstractLTIProviderSystem<CanvasSystemComponent> implements CanvasSystemComponent { // garbage killme
    private static final String GROUP_ID_PARAMETER = "groupIdParameter";

    @Override
    public CanvasSystemComponent update(CanvasSystemComponent system) {
        setGroupIdParameter(system.getGroupIdParameter());
        return super.update(system);
    }

    @Override
    @Configuration(label = "$$label_customGroupIdParameter=Custom Group ID Parameter", order = 20)
    public String getGroupIdParameter() {
        return getStringConfigurationValue(GROUP_ID_PARAMETER);
    }

    @Override
    public void setGroupIdParameter(String groupIdParameter) {
        setStringConfiguration(GROUP_ID_PARAMETER, groupIdParameter);
    }

    @Override
    public Map<String, Object> getConfigurationMap() {
        Map<String, Object> config = super.getConfigurationMap();
        config.put(GROUP_ID_PARAMETER, getGroupIdParameter());
        return config;
    }
}
