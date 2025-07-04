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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningobjects.cpxp.service.ServiceException;
import jakarta.servlet.http.HttpServletResponse;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Exception that encapsulates validation errors
 */
public class ValidationException extends ServiceException implements RestExceptionInterface {

    private List<ValidationInfo> validationMessages = new ArrayList<ValidationInfo>();

    public ValidationException(String propertyName, String propertyValue, String message) {
        super(propertyName + " with value " + propertyValue + " is invalid: " + message);
        ValidationInfo info = new ValidationInfo(propertyName, propertyValue, message);
        validationMessages.add(info);
    }

    protected ValidationException() {
        super("validation errors");
    }

    /**
     * Sets validation messages
     * @param validationMessages messages
     */
    public ValidationException(@Nonnull List<ValidationInfo> validationMessages) {
        super("validation errors");
        this.validationMessages = validationMessages;
    }

    public List<ValidationInfo> getValidationMessages() {
        return validationMessages;
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
        ArrayNode node = JsonNodeFactory.instance.arrayNode();
        for(ValidationInfo info : validationMessages) {
            ObjectNode msg = node.addObject();
            msg.put("property", info.getPropertyName());
            msg.put("value", info.getPropertyValue());
            msg.put("message", info.getMessage());
        }
        return node;
    }

}
