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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.service.exception.RestErrorType;
import com.learningobjects.cpxp.service.exception.RestExceptionInterface;
import org.apache.http.HttpStatus;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.learningobjects.cpxp.component.web.util.JsonNodeFunctions.TO_TEXT_NODE;

/**
 * The HTTP request cannot be serviced because the schema name correlated by the client in the request is incompatible
 * with the {@link RequestBody} parameter for every {@link RequestMapping} method for the request's path and HTTP
 * method.
 */
public class UnsupportedRequestBodyType extends RuntimeException implements RestExceptionInterface {

    private final String unsupportedRequestBodyType;
    private final Collection<String> supportedRequestBodyTypes;
    private final ObjectNode json;

    public UnsupportedRequestBodyType(final String unsupportedRequestBodyType,
            final Collection<String> supportedRequestBodyTypes) {
        this.unsupportedRequestBodyType = unsupportedRequestBodyType;
        this.supportedRequestBodyTypes = List.copyOf(supportedRequestBodyTypes); // defensive copy

        final List<JsonNode> supporteds = this.supportedRequestBodyTypes.stream().map(TO_TEXT_NODE).collect(Collectors.toList());

        json = JacksonUtils.objectNode();
        json.put("message", "Unsupported request body type: '" + unsupportedRequestBodyType + "'");
        json.put("unsupportedRequestBodyType", unsupportedRequestBodyType);
        json.putArray("supportedRequestBodyTypes").addAll(supporteds);


    }

    public String getUnsupportedRequestBodyType() {
        return unsupportedRequestBodyType;
    }

    public Collection<String> getSupportedRequestBodyTypes() {
        return supportedRequestBodyTypes;
    }

    @Override
    public RestErrorType getErrorType() {
        return RestErrorType.CLIENT_ERROR;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.SC_BAD_REQUEST;
    }

    @Override
    public JsonNode getJson() {
        return json;
    }
}
