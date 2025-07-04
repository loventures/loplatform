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
import com.learningobjects.de.web.MediaType;
import org.apache.http.HttpStatus;

import java.util.Collections;
import java.util.List;

public class HttpMediaTypeNotSupportedException extends RuntimeException implements RestExceptionInterface {

    /* the media type that wasn't supported */
    private final MediaType unsupportedType;

    private final List<MediaType> supportedTypes;

    public HttpMediaTypeNotSupportedException(final MediaType unsupportedType, final List<MediaType> supportedTypes) {
        super(unsupportedType + " content is not supported; " + supportedTypes + " are supported types");
        this.unsupportedType = unsupportedType;
        this.supportedTypes = supportedTypes;
    }

    public HttpMediaTypeNotSupportedException(final MediaType unsupportedType) {
        super(unsupportedType + " content is not supported");
        this.unsupportedType = unsupportedType;
        this.supportedTypes = Collections.emptyList();
    }

    public MediaType getUnsupportedType() {
        return unsupportedType;
    }

    public List<MediaType> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public RestErrorType getErrorType() {
        return RestErrorType.CLIENT_ERROR;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE;
    }

    @Override
    public JsonNode getJson() {
        return JsonNodeFactory.instance.objectNode().put("message", getMessage());
    }
}
