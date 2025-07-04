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

package com.learningobjects.cpxp.component.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.learningobjects.cpxp.service.exception.RestErrorType;
import com.learningobjects.cpxp.service.exception.RestExceptionInterface;
import org.apache.http.HttpStatus;

/**
 * An exception from a component validator.
 */
public class ComponentValidationException extends RuntimeException implements RestExceptionInterface {

    public ComponentValidationException(final String msg) {
        super(msg);
    }

    @Override
    public RestErrorType getErrorType() {
        return RestErrorType.BUSINESS_RULE_VIOLATION;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.SC_UNPROCESSABLE_ENTITY;
    }

    @Override
    public JsonNode getJson() {
        return JsonNodeFactory.instance.objectNode().put("message", getMessage());
    }
}
