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

package com.learningobjects.cpxp.service.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.service.ServiceException;
import jakarta.servlet.http.HttpServletResponse;

public class AuthorizationException extends ServiceException implements RestExceptionInterface {

    // json-ly typed, just as bad as string-ly typed.
    private ArrayNode json = JacksonUtils.arrayNode();

    public AuthorizationException(String propertyName, String propertyValue, String message) {
        // should be its own exception type
        super("authentication errors");
        json.addObject().put("property", propertyName).put("value", propertyValue).put("message", message);
    }

    public AuthorizationException(final String message) {
        super(message);
        json.addObject().put("message", message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public RestErrorType getErrorType() {
        return RestErrorType.UNAUTHORIZED_ERROR;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpServletResponse.SC_UNAUTHORIZED;
    }

    @Override
    public JsonNode getJson() {
        return json;
    }

}
