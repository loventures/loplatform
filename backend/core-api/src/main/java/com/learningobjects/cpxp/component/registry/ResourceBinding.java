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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Presence on another annotation means that the targeted annotation expresses an
 * artifact/resource that should be made available in the component environment. For
 * example, the &#64;Schema annotation registers a JSON file in the component environment.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ResourceBinding {

    /**
     * A registry to process and store the resource described by this annotation's
     * target.
     *
     * @return a registry to process and store the resource described by this annotation's
     * target.
     */
    Class<? extends ResourceRegistry<?>> registry();

    /**
     * @return whether or not this binding registers components or not
     */
    boolean component() default true;

}
