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
import org.apache.http.HttpStatus;

/**
 * Means that a client attempted to apply a structural modification to the resource via a HTTP PATCH and the structures assumed to exist did not exist.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5789#section-2.2">PATCH Error Handling</a>
 */
public class PatchStructuralConflictException extends RuntimeException implements RestExceptionInterface{

    public PatchStructuralConflictException(final String message) {
        super(message);
    }

    public PatchStructuralConflictException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PatchStructuralConflictException(final Throwable cause) {
        super(cause);
    }

    @Override
    public RestErrorType getErrorType() {
        return RestErrorType.CLIENT_ERROR;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.SC_CONFLICT;
    }

    @Override
    public JsonNode getJson() {
        return JsonNodeFactory.instance.objectNode().put("message", getMessage());
    }
}
