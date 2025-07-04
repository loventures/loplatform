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

package com.learningobjects.de.web.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.de.web.mediatype.DeMediaTypeProperty;

import java.util.List;

/**
 * Adds extra bean properties to {@link ComponentInterface}s on serialization. Jackson really resists this.
 */
class ComponentInterfaceBeanSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(final SerializationConfig config, final BeanDescription beanDesc,
            final List<BeanPropertyWriter> beanProperties) {

        if (ComponentInterface.class.isAssignableFrom(beanDesc.getClassInfo().getAnnotated())) {

            // load the jackson bean description of the extra properties mix-in class
            final JavaType type = TypeFactory.defaultInstance().constructType(ExtraPropertiesMixIn.class);
            final BeanDescription extrasBeanDesc = new BasicClassIntrospector().forSerialization(config, type, null);
            final List<BeanPropertyDefinition> properties = extrasBeanDesc.findProperties();

            // add new bean property writers for those extra properties
            for (BeanPropertyDefinition extraProp : properties) {

                final String extraPropName = extraProp.getName();
                if (extraPropName.equals(DeMediaTypeProperty.TYPE)) {
                    // use BeanSerializerModifier#orderProperties(...) instead, as this grows to more properties
                    beanProperties.add(0, new TypeBeanPropertyWriter(extraProp));
                }
            }
        }

        return beanProperties;
    }

}
