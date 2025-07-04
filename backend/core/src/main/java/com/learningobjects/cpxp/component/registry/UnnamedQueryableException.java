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

import java.lang.reflect.Method;

public class UnnamedQueryableException extends RuntimeException {

    private UnnamedQueryableException(final String message) {
        super(message);
    }

    public static UnnamedQueryableException forMethod(final Method method) {
        final String msg = "Invalid @Queryable declaration on method: '" + method +
                "'. A @Queryable annotation on a method must either have a 'name()' " +
                "value or be hosted on a @JsonProperty/@JsonView annotated program " +
                "element";
        return new UnnamedQueryableException(msg);
    }

    public static UnnamedQueryableException forClass(final Class<?> clazz) {
        final String msg = "Invalid @Queryable declaration in @QueryableProperties for " +
                "class: '" + clazz + "'. All @Queryables in @QueryableProperties must " +
                "have a 'name()' value";
        return new UnnamedQueryableException(msg);
    }
}
