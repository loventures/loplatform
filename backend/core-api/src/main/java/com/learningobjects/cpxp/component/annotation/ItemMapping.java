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
 * Annotation describing how a primary component interface (category) is
 * mapped to an item type, and optionally how implementations are mapped
 * to data types.
 *
 * Use this, for example, to specify that gradebook implementations should
 * be mapped to items of type "Gradebook", or that group implementations
 * should be selected based on the "type" data field of the "Group" item.
 *
 * If no item mapping is specified, components are stored as item type
 * "Component". If no data mapping is specified, the "componentId" field
 * identifies the implementation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface ItemMapping {
    /** The item type under which components of this interface should be stored. */
    String value();
    /** The data type by which implementations of this interface should be identified. */
    String dataType() default "";
    /** Whether there is just a singleton implementation of the component so no identifier should be stored. */
    boolean singleton() default false;
    /** Whether to identify the component by its schema name rather than classname. */
    boolean schemaMapped() default false;
}
