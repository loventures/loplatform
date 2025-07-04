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

package com.learningobjects.cpxp.component.function;

import com.learningobjects.cpxp.component.annotation.PathVariable;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.de.web.util.UriTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Validates {@link RequestMapping} signatures.
 */
public class RequestMappingSignatureValidator {

    public static final RequestMappingSignatureValidator INSTANCE =
            new RequestMappingSignatureValidator();

    public void validateDeclaringClassRequestMapping(
            final FunctionDescriptor functionDescriptor) {

        final Class<?> declaringClass = functionDescriptor.getDelegate().getDelegateClass();
        final RequestMapping annotation =
            ClassUtils.findAnnotation(declaringClass, RequestMapping.class).orElse(null);

        if ((annotation != null) && (hasEmptyPath(annotation) || hasNonAnyMethod(annotation))) {
            throw new IllegalStateException(
                    "Class level RequestMapping annotation may only declare path " +
                            "element; class: '" +
                            declaringClass + "'");
        }
    }

    public void validate(final FunctionDescriptor functionDescriptor) {

        final Method method = functionDescriptor.getMethod();

        final RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);

        final String methodPath = requestMapping.path();
        final RequestMapping classRequestMapping =
            ClassUtils.findAnnotation(functionDescriptor.getDelegate().getDelegateClass(), RequestMapping.class).orElse(null);
        final String path;
        if (classRequestMapping == null) {
            path = methodPath;
        } else {
            path = classRequestMapping.path() + "/" + methodPath;
        }

        final UriTemplate uriTemplate = new UriTemplate(path);
        final List<String> variableNames = uriTemplate.getVariableNames();

        for (final Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
            for (final Annotation annotation : parameterAnnotations) {
                if (annotation instanceof PathVariable) {
                    final PathVariable pathVariable = (PathVariable) annotation;
                    if (!variableNames.contains(pathVariable.value())) {
                        throw new UnknownPathVariableException(method, pathVariable);
                    }

                }
            }
        }
    }

    private boolean hasEmptyPath(final RequestMapping annotation) {
        return RequestMapping.EMPTY_PATH.equals(annotation.path());
    }

    private boolean hasNonAnyMethod(final RequestMapping annotation) {
        return annotation.method() != com.learningobjects.cpxp.component.web.Method.Any;
    }
}
