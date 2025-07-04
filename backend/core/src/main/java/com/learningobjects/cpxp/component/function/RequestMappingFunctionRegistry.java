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

import com.google.common.base.Joiner;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.DePathSegment;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.UriPathSegment;
import com.learningobjects.cpxp.component.web.exception.HttpRequestMethodNotSupportedException;
import com.learningobjects.cpxp.component.web.exception.UnsupportedRequestBodyType;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A registry of all {@link RequestMapping} methods of a component.
 */
public class RequestMappingFunctionRegistry implements FunctionRegistry {

    private final RequestMappingIndex index = new RequestMappingIndex();

    @Override
    public void register(final FunctionDescriptor function) {

        final RequestMapping requestMapping = function.getAnnotation();

        index.put(requestMapping, function);
    }


    /**
     * Returns the next {@link RequestMapping} method to invoke for the unhandled portion of a request path. Never returns null.
     * <ul>
     *     <li>{@code keys[0]} - {@link Method} HTTP method, must be nonnull</li>
     *     <li>{@code keys[1]} - {@link List} of {@link DePathSegment} that represent the path to lookup, must be nonnull</li>
     *     <li>{@code keys[2]} - {@link String} schema name of the request, may be null</li>
     * </ul>
     * @param keys the lookup keys
     * @return the {@link RequestMapping}-annotated function to invoke
     * @throws ResourceNotFoundException for unmapped paths
     * @throws HttpRequestMethodNotSupportedException for unsupported HTTP methods
     * @throws UnsupportedRequestBodyType for unsupported request body types (as specified by the request's JSON schema correlation)
     */
    @Override
    public FunctionDescriptor lookup(Object... keys) {

        final Method httpMethod = (Method) keys[0];
        final List<DePathSegment> pathSegments = (List<DePathSegment>) keys[1];
        final String typeName = (String) keys[2];

        // we don't use RequestMappingUtil because its joiner puts matrix parameters back
        final List<String> segments =
                pathSegments.stream().map(DePathSegment.GET_URI_PATH_SEGMENT)
                        .map(UriPathSegment.GET_SEGMENT).collect(Collectors.toList());
        final String path = Joiner.on("/").join(segments);

        return index.get(path, httpMethod, typeName);

    }

    @Override
    public Collection<FunctionDescriptor> lookupAll() {
        return index.values();
    }


}
