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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify a validator for a specific component. E.g., UserComponent is validated by UserComponentValidator.
 *
 * As opposed to validators that operate on single properties, ComponentValidator allow use to define (potentially complex) business logic-based validation on the entire component.
 *
 * Note that component validators are themselves components, so multiple implementations can be defined and disabled/enabled or configured. If an implementation is not defined for a validator, then no validation is performed.
 *
 * E.g., one implementation of UserComponentValidator is CarnegieUserComponentValidator, which handles Carnegie-specific constraints (e.g., a teacher ID must be unique within the district). Unless this is enabled in component configuration for a domain, it will not be used to validate a user.
 *
 * Validation occurs after an SRS PATCH request, and can be manually performed by {@code com.learningobjects.component.validator.ComponentValidatorService#validate}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface Validated {

    /**
     * The interface of the validator to use for this component.
     */
    public Class<? extends ComponentValidator> by();
}
