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

import com.learningobjects.cpxp.component.eval.Evaluator;
import com.learningobjects.cpxp.component.function.Function;
import com.learningobjects.cpxp.component.function.FunctionRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

/**
 * Indicates that the annotated method defines a LOHTML tag.
 *
 * When a component class is added as a namespace in a LOHTML
 * document using {@code xmlns:foo="loi.cp..."}, then usages
 * of the tag {@code <foo:method ...>} will be rendered by
 * invoking the annotated method on the component associated
 * with the {@code foo} namespace.
 *
 * Attributes of the tag are passed as {@link Parameter}s of
 * the method. If more attributes than expected are passed to
 * the tag, they are accessible by declaring a parameter
 * {@code @Parameters Map<String, Object> parameters}, which
 * will be populated with the unrecognized parameter values.
 *
 * Parameters may be marked as {@link Required} to throw an
 * exception during rendering if not included.
 *
 * If the tag has a body, it can be accessed by taking a parameter
 * named {@code body} of type {@link Callable}, which can be called
 * any number of times to render the contents of the tag as LOHTML.
 *
 * The method may also accept any parameters evaluable
 * by a known {@link Evaluator}, such as the request object.
 *
 * This annotation is mainly used in legacy LOHTML code and should
 * generally not be used in new development.
 *
 * @see Parameter
 * @see Parameters
 * @see TagInstance
 * @see Evaluator
 * @see FunctionRegistry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Function
public @interface Tag {
}
