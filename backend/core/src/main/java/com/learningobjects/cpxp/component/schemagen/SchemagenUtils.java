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

package com.learningobjects.cpxp.component.schemagen;

import com.learningobjects.cpxp.component.annotation.SchemaNamePrefix;
import com.learningobjects.cpxp.util.StringUtils;

import javax.validation.groups.Default;
import java.lang.annotation.Annotation;

public class SchemagenUtils {

    /**
     * Get the {@link SchemaNamePrefix} for the given group. If the group does not have an explicit {@link
     * SchemaNamePrefix} then it the simple name of the group class is used as the prefix, unless the group is {@link
     * javax.validation.groups.Default} which has no prefix.
     *
     * @return the {@link SchemaNamePrefix} for the given group.
     */
    public static SchemaNamePrefix getSchemaNamePrefix(final Class<?> group) {

        final SchemaNamePrefix schemaNamePrefix;

        if (Default.class.equals(group)) {
            schemaNamePrefix = new SchemaNamePrefixImpl("");
        } else {

            final SchemaNamePrefix annotation = group.getAnnotation(SchemaNamePrefix.class);

            if (annotation == null) {
                schemaNamePrefix = new SchemaNamePrefixImpl(StringUtils.toLowerCaseFirst(group.getSimpleName()));
            } else if (StringUtils.isEmpty(annotation.value())) {
                throw new IllegalStateException("Group: '" + group + "' cannot use empty name prefix. The empty name " +
                        "prefix is reserved for the Default group");
            } else {
                schemaNamePrefix = annotation;
            }
        }

        return schemaNamePrefix;

    }

    public static String getSchemaName(final String baseName, final Class<?> group) {

        final String schemaName;

        final SchemaNamePrefix prefix = getSchemaNamePrefix(group);

        final String casedBaseName;
        if (StringUtils.isEmpty(prefix.value())) {
            casedBaseName = baseName;
        } else {
            casedBaseName = StringUtils.toUpperCaseFirst(baseName);
        }

        schemaName = prefix.value() + casedBaseName;

        return schemaName;
    }

    static class SchemaNamePrefixImpl implements SchemaNamePrefix {

        private final String value;

        public SchemaNamePrefixImpl(final String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return SchemaNamePrefix.class;
        }
    }
}
