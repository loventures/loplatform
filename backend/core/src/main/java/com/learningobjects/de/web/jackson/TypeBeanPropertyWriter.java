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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Annotations;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;

/**
 * A bean property writer for the extra {@code _type} property added to {@link ComponentInterface}s.
 */
public class TypeBeanPropertyWriter extends BeanPropertyWriter {

    private static final JavaType STRING_TYPE = TypeFactory.defaultInstance().constructType(String.class);

    public TypeBeanPropertyWriter(final BeanPropertyDefinition propDef) {
        this(propDef, propDef.getGetter(), null, STRING_TYPE, null, null, STRING_TYPE, false, null);
    }

    private TypeBeanPropertyWriter(final BeanPropertyDefinition propDef, final AnnotatedMember member,
            final Annotations contextAnnotations, final JavaType declaredType, final JsonSerializer<?> ser,
            final TypeSerializer typeSer, final JavaType serType, final boolean suppressNulls,
            final Object suppressableValue) {
        super(propDef, member, contextAnnotations, declaredType, ser, typeSer, serType, suppressNulls,
                suppressableValue);
    }

    @Override
    public void serializeAsField(final Object bean, final JsonGenerator jgen, final SerializerProvider prov)
            throws Exception {
        if (bean instanceof SerializeAsSubtype) {
            // TODO: I was going to return the schema name in a _subtype property but omg the effort
            return;
        }

        final ComponentInterface component = (ComponentInterface) bean;

        final String type = ComponentSupport.getSchemaName(component);

        final ExtraPropertiesMixIn extras = new ExtraPropertiesMixIn(type);

        // calls `getType` on `extras` and serializes the return value
        super.serializeAsField(extras, jgen, prov);
    }
}
