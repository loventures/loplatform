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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodAccessor extends PropertyAccessor {
    private final Method _method;

    public MethodAccessor(Method method) {
        _method = method;
        _method.setAccessible(true);
    }

    @Override
    public Object get(Object bean, String property) throws
            PropertyAccessorException {
        try {
            return _method.invoke(bean);
        } catch (InvocationTargetException ex) {
            Throwable t = ex.getCause();
            throw new PropertyAccessorException("Error invoking " + _method +
                    ": " + t.getMessage(), t);
        } catch (IllegalAccessException ex) {
            throw new PropertyAccessorException("Illegal access invoking " +
                    _method + ": " + ex.getMessage(), ex);
        }
    }
}
