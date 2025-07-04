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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningobjects.cpxp.service.exception.RestErrorType;
import com.learningobjects.cpxp.service.exception.RestExceptionInterface;
import jakarta.servlet.http.HttpServletResponse;

public class UnsupportedVersionException extends RuntimeException implements RestExceptionInterface {

    private final String unsupportedVersion;

    public UnsupportedVersionException() {
        super("The required 'version' path variable is absent.");
        unsupportedVersion = null;
    }

    public UnsupportedVersionException(final String unsupportedVersion) {
        super("The version '" + unsupportedVersion + "' is not supported by this resource");
        this.unsupportedVersion = unsupportedVersion;
    }

    public String getUnsupportedVersion() {
        return unsupportedVersion;
    }

    @Override
    public RestErrorType getErrorType() {
        return RestErrorType.UNSUPPORTED_VERSION;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpServletResponse.SC_BAD_REQUEST;
    }

    @Override
    public JsonNode getJson() {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("property", "version");
        json.put("value", unsupportedVersion);
        json.put("message", this.getMessage());
        return json;
    }
}
