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

import com.learningobjects.cpxp.component.registry.Binding;
import com.learningobjects.cpxp.component.registry.ExactRegistry;

import java.lang.annotation.*;

/**
 * Associates the function registry type to a function type (aka {@link FunctionInstance}
 * type). If omitted on the function type, then {@link DefaultFunctionRegistry} is used.
 *
 * Use this if functions of this type should not be looked up by simple string name equality
 * but, for example, by some more complex mechanism such as by argument or generic type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Binding(property = "annotations", component = false, registry = ExactRegistry.class)
public @interface FunctionBinding {
    /** The registry class to use for this function instance type. */
    Class<? extends FunctionRegistry> registry();

    /** The annotations that this function instance supports. */
    Class<? extends Annotation>[] annotations() default {};
}
