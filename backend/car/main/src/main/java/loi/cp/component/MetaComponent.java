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

package loi.cp.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.registry.Bound;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.script.ScriptService;
import com.learningobjects.cpxp.util.ClassUtils;
import org.apache.commons.lang3.BooleanUtils;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/** This is a fake component that describes a component. */
@Component
public class MetaComponent extends AbstractComponent implements ComponentComponent {

    @Inject
    private ScriptService _scriptService;

    @Infer
    private DomainDTO _domain;

    @Inject
    private ComponentEnvironment componentEnvironment;

    private ComponentInstance _instance;

    public void init(ComponentDescriptor component) {
        _instance = component.getInstance(null, null);
    }

    @Override
    public String getIdentifier() {
        return _instance.getIdentifier();
    }

    @Override
    public String getName() {
        return _instance.getName();
    }

    @Override
    public String getSchema() {
        return ClassUtils.findAnnotation(_instance.getComponent().getCategory(), Schema.class)
          .map(Schema::value)
          .orElse(null);
    }

    @Override
    public Map<String, ComponentInterfaceJson> getInterfaces() {
        ComponentDescriptor component = _instance.getComponent();
        Map<String, ComponentInterfaceJson> interfaces = new HashMap<>();
        for (Class<? extends ComponentInterface> iface : component.getInterfaces()) {
            if (iface.getPackage().equals(ComponentInterface.class.getPackage())) {
                continue;
            }
            Boolean primary = iface.equals(component.getCategory()) ? true : null;
            Bound bound = iface.getAnnotation(Bound.class);
            Annotation binding = (bound == null) ? null : component.getAnnotation(bound.value());
            // ZOMG: Map because annotations are otherwise not serialized by jackson
            // because of the nonstandard method names
            Map<String, Object> bindingMap = ClassUtils.marshalAnnotation(binding);
            interfaces.put(iface.getName(), new ComponentInterfaceJson(primary, bindingMap));
        }
        return interfaces;
    }

    @Override
    public String getDescription() {
        return _instance.getDescription();
    }

    @Override
    public ComponentInterface getInstance() {
        return _instance.getInstance(ComponentInterface.class);
    }

    @Override
    public ObjectNode getConfiguration() {
        return componentEnvironment
          .getJsonConfiguration(getIdentifier(), ObjectNode.class)
          .orElse(ComponentSupport.getObjectMapper().createObjectNode());
    }

    @Override
    public JsonNode updateConfiguration(ObjectNode newConfig, Boolean merge) {
        if (BooleanUtils.isTrue(merge)) {
            ObjectNode config = getConfiguration();
            config.setAll(newConfig);
            _scriptService.setComponentConfiguration(_domain.getId(), getIdentifier(), config.toString());
        } else {
            _scriptService.setComponentConfiguration(_domain.getId(), getIdentifier(), newConfig.toString());
        }
        return getConfiguration();
    }
}

