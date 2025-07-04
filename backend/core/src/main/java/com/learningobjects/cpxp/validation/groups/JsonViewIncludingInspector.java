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

package com.learningobjects.cpxp.validation.groups;

import com.fasterxml.jackson.annotation.JsonView;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.PropertyDescriptorUtils;

import javax.validation.groups.Default;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Finds the validation groups of a class. Considers the views of a {@link JsonView} to be validation groups.
 */
public class JsonViewIncludingInspector extends AbstractValidationGroupInspector {

    public JsonViewIncludingInspector(final Class<?>... forcedGroups) {
        super(forcedGroups);
    }

    @Override
    public Set<Class<?>> findGroups(final Class<?> clazz) {

        final Set<Class<?>> groups = new HashSet<>();
        groups.add(Default.class);
        groups.addAll(getForcedGroups());

        for (final java.beans.PropertyDescriptor jbpd : ClassUtils.getAllProperties(clazz)) {

            final Method method = PropertyDescriptorUtils.getMethod(jbpd);

            final JsonView jsonView = method.getAnnotation(JsonView.class);
            if (jsonView != null) {
                groups.addAll(Arrays.asList(jsonView.value()));
            }
        }

        // this will come back to bite me
        groups.remove(clazz);

        return Collections.unmodifiableSet(groups);
    }
}
