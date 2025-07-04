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

/**
 * A registry for class keys. This will search for registrations under a specified class
 * or any of it superclasses.
 */
public class ClassKeyRegistry<T> extends ExactRegistry<T> {
    @Override
    protected T get(Object key) {
        T t = null;
        for (Class<?> cls = (Class<?>) key; (t == null) && (cls != null); cls = cls.getSuperclass()) {
            t = super.get(cls);
        }
        return t;
    }
}
