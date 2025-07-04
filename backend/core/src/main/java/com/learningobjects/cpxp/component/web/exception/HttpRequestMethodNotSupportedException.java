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

package com.learningobjects.cpxp.component.web.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.service.exception.RestErrorType;
import com.learningobjects.cpxp.service.exception.RestExceptionInterface;
import jakarta.servlet.http.HttpServletResponse;

public class HttpRequestMethodNotSupportedException extends RuntimeException implements RestExceptionInterface {

    private final Method method;

    public HttpRequestMethodNotSupportedException(Method method) {
        super("Request method '" + method.name() + "' not supported");
        this.method = method;
    }

    @Override
    public RestErrorType getErrorType() {
        //with no body there is no error type
        return null;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpServletResponse.SC_METHOD_NOT_ALLOWED;
    }

    @Override
    public JsonNode getJson() {
        //we don't have a body
        return null;
    }
}
