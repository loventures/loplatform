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

package com.learningobjects.cpxp.component.registry;

import com.learningobjects.cpxp.component.internal.DelegateDescriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an annotation as a binding annotation.
 *
 * <p>A binding annotation is an annotation whose declaration on a subtype of a {@link
 * Bound} type will cause that subtype to be registered in the {@link Bound}'s registry.
 * The details of how the subtype is bound and, subsequently, how it can be retrieved, is
 * left to the registry type. A common scenario is for elements on the binding annotation
 * to be used to build a key for the subtype, and the registry instance is like a map.
 *
 * <p>Note that the subtype's {@link DelegateDescriptor} is what is actually bound in the
 * registry, not the subtype's {@link Class}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Binding {

    String property() default "";

    Class<? extends Registry> registry();

    boolean component() default true;
}
