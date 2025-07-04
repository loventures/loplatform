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

import com.learningobjects.cpxp.component.function.Function;
import com.learningobjects.cpxp.component.web.Method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rpc defines a method as able to be invoked through a URL - e.g. a REST call
 * The actual URL is defined by the 'binding' parameter - e.g. @Rpc(binding = "/api/quiz/assess")
 * If a binding is not specified the URL will be dynamically determined and will be
 * of the form "/root/method" where 'root' is either specified by a @ServletBinding
 * annotation on the class or is "/control/component/fully qualified class name", and 'method' is either
 * specified by the 'name' parameter or is the name of the annotated method.
 * So without any explicitly defined parameters the URL would look like /control/component/com.loix.quiz.Quiz/assessQuiz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Function(global = "binding")
public @interface Rpc {

    /**
     * The HTTP method to use when making the call (e.g. GET or POST)
     * One of the com.learningobjects.cpxp.component.web.Method enum.
     */
    Method method() default Method.View;

    /**
     * If no binding is specified, this specifies the end part of the URL.
     * If not provided, the method name is used.
=     */
    String name() default "";

    /**
     * Specified the URL this method should be bound to.
     * If none is provided will dynamically construct one (see class comments)
     */
    String binding() default "";

}
