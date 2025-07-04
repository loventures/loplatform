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

import com.learningobjects.cpxp.component.eval.Evaluate;

import jakarta.servlet.http.HttpSession;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a value from the servlet session by name.
 *
 * The value is the same one returned by {@link HttpSession#getAttribute(String)},
 * with the value parsed if the expected type is Long or Boolean and the
 * session contains a String.
 *
 * If annotating a field, the field will be assigned the value at object creation;
 * if annotating a parameter, it will be passed as the parameter value when invoked
 * as an RPC.
 *
 * @see HttpSession
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Evaluate
public @interface Session {
    /**
     * The name of the session variable to inject.
     *
     * If omitted, the name of the annotated field or parameter will be used instead.
     */
    String name() default "";
}
