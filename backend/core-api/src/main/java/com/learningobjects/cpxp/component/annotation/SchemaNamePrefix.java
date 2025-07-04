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
 * When a schema is generated for a type annotated with @Schema, this annotation can be
 * used to customize the name of the schema file, by placing a prefix in front.
 * Presently unused. For example, there is a Writable version of a schema (by using
 * the Writable JsonView). The schema for that view will be called writableFoo.json,
 * since the view name is "writable". If the Writable view is annotated with
 * &#64;SchemaNamePrefix then the prefix is the one specified.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaNamePrefix {

    /**
     * The prefix to use
     */
    String value();
}
