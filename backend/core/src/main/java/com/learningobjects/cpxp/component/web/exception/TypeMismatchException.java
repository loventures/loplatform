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

import com.learningobjects.cpxp.service.exception.ValidationException;

public class TypeMismatchException extends ValidationException {

    /** the mismatched value */
    private final String value;

    private final String propertyName;


    private final Class<?> requiredType;

    public TypeMismatchException(final String value, final String propertyName, final Class<?> requiredType) {
        super(propertyName, value, "Failed to convert '" + value + "' to type '" + requiredType.getName() + "'");
        this.value = value;
        this.propertyName = propertyName;
        this.requiredType = requiredType;
    }

    public String getValue() {
        return value;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getRequiredType() {
        return requiredType;
    }
}
