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

package com.learningobjects.cpxp.dto;

import com.learningobjects.cpxp.service.query.Direction;
import com.learningobjects.cpxp.service.query.Function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to define a query as a method on the facade. The query is expressed as constraints and parameters to both the method and annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface FacadeQuery {
    /**
     * @return the facade method group to which this method belongs, if named
     * in a non-standard way.
     */
    public String group() default "";
    /**
     * @return whether to use the query cache
     */
    public boolean cache() default true;

    public boolean debug() default false;

    /**
     * query the whole domain...
     */
    public boolean domain() default false;

    public Function function() default Function.NONE;

    public String projection() default "";

    public String orderType() default "";

    public Direction orderDirection() default Direction.ASC;

    public Function orderFunction() default Function.NONE;
}
