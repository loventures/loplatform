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

package com.learningobjects.cpxp.component.function;

import com.learningobjects.cpxp.component.annotation.PathVariable;
import com.learningobjects.cpxp.component.annotation.RequestMapping;

import java.lang.reflect.Method;

/**
 * Thrown on detection of a {@link PathVariable} whose name is not found in the host
 * method's {@link RequestMapping}
 */
public class UnknownPathVariableException extends RuntimeException {

    public UnknownPathVariableException(final Method method,
            final PathVariable pathVariable) {
        super("Unknown path variable: '" + pathVariable
                .value() + "'; @RequestMapping path: '" + method.getAnnotation(
                RequestMapping.class).path() + "'; method: '" + method + "'");
    }

}
