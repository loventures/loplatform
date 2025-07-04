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

import java.lang.annotation.*;

import com.learningobjects.cpxp.service.query.Direction;
import com.learningobjects.cpxp.service.query.Function;

/**
 * Annotates methods on a facade that operate of children facades.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FacadeChild {
    /**
     * @return the item type to which a facade method applies. If unset, the
     * child facade must identify the item type.
     */
    public String value() default "";

    /**
     * @return the data type by which to order children
     */
    public String[] orderType() default {};

    public Function[] orderFunction() default {};

    /**
     * @return the direction in which to order children
     */
    public Direction[] direction() default {};

    public boolean trash() default false;
}
