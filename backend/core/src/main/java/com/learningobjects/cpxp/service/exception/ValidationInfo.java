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


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;

import javax.annotation.Nonnull;

/**
 * Information about the validation errors
 */
public class ValidationInfo {

    @JsonProperty("property")
    private String propertyName;

    @JsonProperty("value")
    private String propertyValue;

    @JsonProperty
    private String message;

    public ValidationInfo(String propertyName, String propertyValue, String message) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.message = message;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static Predicate<ValidationInfo> hasPropertyName(
            @Nonnull final String propertyName) {
        return new Predicate<ValidationInfo>() {

            @Override
            public boolean apply(final ValidationInfo v) {
                return propertyName.equals(v.getPropertyName());
            }
        };
    }
}
