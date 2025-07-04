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

package com.learningobjects.cpxp.component.util;

import java.lang.reflect.Field;

class FieldAccessor extends PropertyAccessor {
    private final Field _field;

    public FieldAccessor(Field field) {
        _field = field;
        _field.setAccessible(true);
    }

    @Override
    public Object get(Object bean, String property) {
        try {
            return _field.get(bean);
        } catch (Exception ex) {
            throw new RuntimeException("Field property access error: " + _field, ex);
        }
    }
}
