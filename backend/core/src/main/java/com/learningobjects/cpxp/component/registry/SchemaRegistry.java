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

package com.learningobjects.cpxp.component.registry;

import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.schemagen.SchemagenUtils;
import com.learningobjects.cpxp.validation.groups.JsonViewIncludingInspector;
import com.learningobjects.cpxp.validation.groups.ValidationGroupInspector;
import com.learningobjects.cpxp.validation.groups.Writable;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * A registry of JSON schema names.
 */
public class SchemaRegistry implements ResourceRegistry<SchemaRegistry.Registration> {

    /**
     * Sub schemas, keyed by sub schema name, a subset of those in classpath:/schema/json/**.
     */
    private final Map<String, Registration> enabledSubSchemas = new HashMap<>();

    private final ValidationGroupInspector groupInspector=new JsonViewIncludingInspector(Writable.class);

    @Override
    public void register(final Annotation annotation, final Class<?> clazz) {

        checkState(annotation instanceof Schema);

        final String subSchemaName = ((Schema) annotation).value();

        for (final Class<?> group : groupInspector.findGroups(clazz)) {
            final String subSchemaVariantName = SchemagenUtils.getSchemaName(subSchemaName, group);
            enabledSubSchemas.put(subSchemaVariantName, new Registration(clazz, subSchemaVariantName));
        }

        enabledSubSchemas.put(subSchemaName, new Registration(clazz, subSchemaName));

    }

    @Nullable
    @Override
    public Registration lookup(final Object... keys) {

        checkState(keys.length > 0);
        checkState(keys[0] instanceof String);

        final String subSchemaName;

        if (keys.length == 2) {
            checkState(keys[1] instanceof Class);
            final Class<?> group = (Class<?>) keys[1];
            subSchemaName = SchemagenUtils.getSchemaName((String) keys[0], group);
        } else {
            subSchemaName = (String) keys[0];
        }

        return enabledSubSchemas.get(subSchemaName);
    }

    @Override
    public Collection<Registration> lookupAll() {
        return enabledSubSchemas.values();
    }

    @Override
    public void merge(final ResourceRegistry<Registration> right) {

        if (right != null && right instanceof SchemaRegistry) {
            final SchemaRegistry that = (SchemaRegistry) right;
            this.enabledSubSchemas.putAll(that.enabledSubSchemas);
        }
    }

    public static class Registration {

        private final Class<?> clazz;
        private final String schemaName;

        private Registration(final Class<?> clazz, final String schemaName) {
            this.clazz = clazz;
            this.schemaName = schemaName;

        }

        public String getSchemaName() {
            return schemaName;
        }

        /**
         * @return the {@link Class} declaring the {@link Schema} annotation.
         */
        public Class<?> getSchemaClass() {
            return clazz;
        }

    }

}
