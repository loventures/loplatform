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

import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.de.web.util.UriTemplate;

/**
 * Thrown to prevent the component environment from skipping the registration of duplicating request mappings (but
 * different {@link java.lang.reflect.Method}s)
 */
public class AmbiguousRequestMappingException extends RuntimeException {

    private final FunctionDescriptor original;
    private final FunctionDescriptor duplicating;
    private final UriTemplate uriTemplate;
    private final Method httpMethod;
    private final String typeName;

    public AmbiguousRequestMappingException(final FunctionDescriptor original, final FunctionDescriptor duplicating,
            final UriTemplate uriTemplate, final Method httpMethod, final String typeName) {
        super("Ambiguous @RequestMapping declarations; '" + original + "' and '" + duplicating + "' are bound to the " +
                "same path, HTTP method, and @RequestBody type; path='" + uriTemplate + "'; httpMethod='" +
                httpMethod + "'; " + "typeName='" + typeName + "'. The component environment has not fully loaded. To" +
                " resolve this exception, turn off one of the offending components, or change one's HTTP API.");
        this.original = original;
        this.duplicating = duplicating;
        this.uriTemplate = uriTemplate;
        this.httpMethod = httpMethod;
        this.typeName = typeName;
    }

    public FunctionDescriptor getOriginal() {
        return original;
    }

    public FunctionDescriptor getDuplicating() {
        return duplicating;
    }

    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    public Method getHttpMethod() {
        return httpMethod;
    }

    public String getTypeName() {
        return typeName;
    }
}
