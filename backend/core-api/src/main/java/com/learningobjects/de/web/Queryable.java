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

package com.learningobjects.de.web;

import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.service.data.DataTypedef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a property getter/setter on any POJO type is a getter/setter for a
 * datum in the data model. This is most often used on {@link Component} types to
 * associate a getter to a Constants string (skipping facades). To be
 * filterable/orderable, a property has to host {@link Queryable}.
 *
 * <p>Yes, this whole idea is messy. When the real problem is fixed, this can go away.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface Queryable {

    public static final String USE_JACKSON_NAME = "";

    /**
     * The name of the property in the POJO type. Omit to use the Jackson property name
     *
     * @return the name of the property in the POJO type
     */
    String name() default USE_JACKSON_NAME;

    /**
     * @return {@link DataTypedef} string that the annotated method definition gets/sets
     */
    String dataType() default "";

    /**
     * @return The {@link ComponentInterface} or [Finder] against which this query is joining.
     * This is technically discoverable as the return type of the annotated method but this
     * change is less invasive.
     */
    Class<?> joinComponent() default ComponentInterface.class;

    /**
     * A custom handler for this query property if {@link #dataType()} is unset.
     */
    Class<? extends QueryHandler> handler() default QueryHandler.class;

    /**
     * @return options for the filtering/sorting
     */
    Trait[] traits() default {};

    public static enum Trait {

        /**
         * ignore case
         */
        CASE_INSENSITIVE,

        /**
         * throw validation exception when annotated property is used in an ordering
         */
        NOT_SORTABLE,

        /**
         * throw validation exception when annotated property is used in a predicate
         */
        NOT_FILTERABLE,

        /**
         * do not stem words for text search
         */
        NO_STEMMING;

        public static final Trait[] NO_TRAITS = new Trait[0];

    }

}
