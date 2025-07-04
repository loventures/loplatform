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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.learningobjects.cpxp.service.exception.RestErrorType;
import com.learningobjects.cpxp.service.exception.RestExceptionInterface;
import jakarta.servlet.http.HttpServletResponse;

public class HttpMessageNotReadableException extends RuntimeException implements RestExceptionInterface{


    public HttpMessageNotReadableException(final Throwable cause) {
        super(cause);
    }

    public HttpMessageNotReadableException(final String message) {
        super(message);
    }

    public HttpMessageNotReadableException(final String message,
                                           final Throwable cause) {
        super(message, cause);
    }

    @Override
    public RestErrorType getErrorType() {
        return RestErrorType.VALIDATION_ERROR;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpServletResponse.SC_BAD_REQUEST;
    }

    @Override
    public JsonNode getJson() {
        return JsonNodeFactory.instance.objectNode().put("message", getMessage());
    }
}
