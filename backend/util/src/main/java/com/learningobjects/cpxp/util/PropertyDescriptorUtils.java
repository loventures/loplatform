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

package com.learningobjects.cpxp.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public final class PropertyDescriptorUtils {

    public static Method getMethod(final PropertyDescriptor propertyDescriptor) {

        final Method method;
        final Method readMethod = propertyDescriptor.getReadMethod();
        if (readMethod == null) {
            final Method writeMethod = propertyDescriptor.getWriteMethod();
            if (writeMethod == null) {
                /* try to find the field? */
                throw new IllegalArgumentException("property " + propertyDescriptor.getName() + " has neither read nor write method");
            } else {
                method = writeMethod;
            }
        } else {
            method = readMethod;
        }
        return method;
    }

    public static Type getPropertyType(final PropertyDescriptor propertyDescriptor) {

        final Type type;

        final Method readMethod = propertyDescriptor.getReadMethod();
        if (readMethod != null) {
            type = readMethod.getGenericReturnType();
        } else {
            final Method writeMethod = propertyDescriptor.getWriteMethod();

            if (writeMethod != null) {
                // if write method exists it must have a single parameter according to JavaBeans spec
                type = writeMethod.getGenericParameterTypes()[0];
            } else {

                type = propertyDescriptor.getPropertyType();
            }

        }
        return type;
    }

}
