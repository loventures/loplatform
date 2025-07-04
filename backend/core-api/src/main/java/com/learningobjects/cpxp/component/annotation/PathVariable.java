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

package com.learningobjects.cpxp.component.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method parameter that receives the value of a URL path variable. The path
 * variable value in the URL is of course a String and it will be converted to the
 * type of the annotated parameter by finding a suitable StringConverter.
 *
 * <p>For example</p>
 * <pre>
 * &#64;RequestMapping(path = "foos/{id}")
 * void getFoo(@PathVariable("id") final Long id)
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {

    /**
     * The name of the path variable in the URL template of the method's
     * RequestMapping#path. This value is always required because bytecode for interfaces
     * never contains parameter names.
     */
    String value();

}
