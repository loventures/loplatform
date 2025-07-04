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

import com.google.common.base.Throwables;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;

public abstract class BasicRegistry<T> implements Registry<Annotation, T> {
    @Override
    public void register(Annotation annotation, T object) {
        Object key = getKey(annotation, object);
        if (key instanceof Object[]) {
            for (Object subkey : (Object[]) key) {
                put(subkey, object);
            }
        } else {
            put(key, object);
        }
    }

    protected Object getKey(Annotation annotation, T object) {
        try {
            Binding binding = annotation.annotationType().getAnnotation(Binding.class);
            return StringUtils.isEmpty(binding.property()) ? null : annotation.getClass().getMethod(binding.property()).invoke(annotation);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    @Override
    public T lookup(Object[] keys) {
        return (keys.length == 1) ? get(keys[0]) : null;
    }

    protected abstract void put(Object key, T object);

    protected abstract T get(Object key);
}
