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

package loi.cp.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.learningobjects.cpxp.service.exception.RestErrorType;

/**
 * Response body for an error (400/500 responses)
 */
public class ErrorResponseBody {

    @JsonProperty
    private final RestErrorType type;

    @JsonProperty
    private final JsonNode messages;

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String guid;

    public ErrorResponseBody(final RestErrorType type) {
        this(type, MissingNode.getInstance(), null);
    }

    public ErrorResponseBody(final RestErrorType type, final JsonNode json){
        this(type, json, null);
    }

    public ErrorResponseBody(final RestErrorType type, final JsonNode messages, final String guid) {
        this.type = type;
        this.messages = messages;
        this.guid = guid;
    }

    public RestErrorType getType() {
        return type;
    }

}
