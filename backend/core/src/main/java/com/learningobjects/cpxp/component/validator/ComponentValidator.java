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

import com.learningobjects.cpxp.component.ComponentInterface;

/**
 * Use to create component validator components, e.g., UserValidator.
 * Implementations of these components are in turn defined to perform validation with specific business logic, e.g., CarnegieUserValidator.
 */
public interface ComponentValidator <C extends ComponentInterface> extends ComponentInterface {

    /**
     * Validate an instance of a component using this component validator.
     * @throws com.learningobjects.cpxp.component.validator.ComponentValidationException Thrown if there is a validation error.
     */
    public void validate(final C component) throws ComponentValidationException;
}
