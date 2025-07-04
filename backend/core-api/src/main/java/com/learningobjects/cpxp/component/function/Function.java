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

import com.learningobjects.cpxp.component.ComponentDescriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an annotation as a function annotation.
 *
 * <p>A function annotation is an annotation whose declaration on a method will cause that
 * method to be registered in a registry in the method's {@link ComponentDescriptor}. The
 * method can then be looked up by the rules of its function type (aka. its {@link
 * FunctionInstance} type).
 *
 * <p>The function's type is the return value of {@link #value()}. The function type
 * concerns itself primarily with invoking the method once it is looked up. But the
 * function type also sets the function registry type via its {@link FunctionBinding}.
 * The function registry controls the details of registration and lookup.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Function {

    /**
     * The function type of the function annotation. The function annotation is the
     * annotation hosting this {@link Function}. If unset, the annotation is bound
     * on to function instance by the {@link FunctionBinding#annotations} value.
     *
     * @return the function type of the function annotation
     */
    Class<? extends FunctionInstance> value() default FunctionInstance.class;

    /**
     * The name of the method on the annotated annotation that returns the global
     * function name, if the function type supports global names. A globally-named
     * function can be looked up irrespective of an actual component instance.
     */
    String global() default "";
}
