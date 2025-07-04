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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class ReflectionUtils {

    /**
     * Finds the first method parameter of the given method that declares an annotation of the given type and returns
     * the parameter's generic type, or {@code null} if no such parameter exists. The iteration order of method
     * parameters is the order defined by {@link Method#getParameterAnnotations()}.
     *
     * @param method         the method whose parameter list is searched
     * @param annotationType the annotation to look for
     * @return the generic type as returned by {@link Method#getGenericParameterTypes()} of the first parameter that
     * declares an annotation of the given type.
     */
    public static Type findGenericParameterWithAnnotation(final Method method,
                                                          final Class<? extends Annotation> annotationType) {

        for (int i = 0; i < method.getParameterAnnotations().length; i++) {
            final Annotation[] annotations = method.getParameterAnnotations()[i];
            for (final Annotation parameterAnnotation : annotations) {
                if (annotationType.isAssignableFrom(parameterAnnotation.annotationType())) {
                    return method.getGenericParameterTypes()[i];
                }
            }
        }

        return null;
    }

    /**
     * Finds the first method parameter of the given method that declares an annotation of the given type and returns
     * the parameter's type, or {@code null} if no such parameter exists. The iteration order of method parameters is
     * the order defined by {@link Method#getParameterAnnotations()}.
     *
     * @param method         the method whose parameter list is searched
     * @param annotationType the annotation to look for
     * @return the type as returned by {@link Method#getParameterTypes()} of the first parameter that declares an
     * annotation of the given type.
     */
    public static Class<?> findParameterWithAnnotation(final Method method,
                                                       final Class<? extends Annotation> annotationType) {
        for (int i = 0; i < method.getParameterAnnotations().length; i++) {
            final Annotation[] annotations = method.getParameterAnnotations()[i];
            for (final Annotation parameterAnnotation : annotations) {
                if (annotationType.isAssignableFrom(parameterAnnotation.annotationType())) {
                    return method.getParameterTypes()[i];
                }
            }
        }

        return null;
    }
}
