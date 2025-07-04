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

import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.component.function.Function;
import com.learningobjects.cpxp.component.query.ApiPage;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.Mode;
import com.learningobjects.cpxp.service.domain.DomainDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a Java method to an HTTP method and path.
 *
 * Methods that host {@link RequestMapping} can have very flexible signatures. In any
 * order, the signature can declare the following parameter types:
 * <ul>
 *     <li>{@link HttpServletRequest}/{@link HttpServletResponse} objects. Use as a
 *     Feature of Last Resort. Neither of these objects
 *     will be wrapped in any protective wrappers. A component author can commit the
 *     response. That will probably break something. If you are using these, let me
 *     know. What you are doing should probably be integrated into the framework
 *     somehow.</li>
 *     <li>{@link WebRequest} A richer {@code HttpServletRequest}</li>
 *     <li>{@link PathVariable} annotated parameter types. These provide access to URI
 *     template variables defined in {@link #path()}. {@code PathVariable} parameter
 *     types can be any type that has a {@link StringConverterComponent} enabled in the
 *     domain. String and Long do.</li>
 *     <li>{@link HttpSession} The container-provided session object</li>
 *     <li>{@link HttpContext} An object wrapping request, response, and session
 *     objects, for convenience.</li>
 *     <li>{@link ServiceMeta} The current {@link ServiceMeta} object providing
 *     information about the node and cluster that the request is being processed by.</li>
 *     <li>{@link DomainDTO} The current domain.</li>
 *     <li>{@link RequestBody} annotated parameter type. This provides access to the
 *     HTTP message body. The message body is converted to the parameter type using
 *     {@link HttpMessageConverter}s.</li>
 *     <li>{@link QueryParam} annotated parameter types. These provide access to query
 *     string variables in the HTTP message. {@code QueryParam} parameter types can be
 *     any type that has a {@link StringConverterComponent} enabled in the domain.
 *     String and Long do.</li>
 *     <li>{@link ApiQuery} This object will hold the values of pagination, filtering,
 *     and sorting matrix parameters.</li>
 *     <li>{@link ApiPage} This object will hold the values of pagination matrix
 *     parameters. This will be deprecated soon. Use {@code ApiQuery}.</li>
 * </ul>
 *
 * <p>When used to annotated a type, {@link #path()} is the only legal element. A runtime
 * exception will occur when the domain is loaded if any other element is specified.</p>
 *
 * <p>When used on a method, at least one of {@link #path()} or {@link #method()} must
 * be provided. A runtime exception will occur when the domain is loaded if neither
 * element is specified. When {@link #path()} is empty, the declaring class of the method
 * must have a {@code RequestMapping}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Function
public @interface RequestMapping {

    public static final String EMPTY_PATH = "";

    /**
     * The <a href="https://tools.ietf.org/html/rfc6570">URL Template</a> that this
     * method is bound to.
     *
     * <p>When a Java method is in scope, it will be invoked when its {@code path}
     * matches the most of the remaining request-URI compared to all other Java methods
     * also in scope. A Java method is in scope when the current component in the
     * chaining
     * sequence declares the method in its component descriptor. When {@link #method()}
     * is not {@link Method#Any}, the URI template is actually the concatenation of the
     * class/interface {@code RequestMapping} and the method {@code RequestMapping}.</p>
     *
     * <p> A path should always be relative (i.e. never start with a slash). </p>
     *
     * @return the HTTP URI path that this binding is bound to
     */
    String path() default EMPTY_PATH;

    /**
     * The HTTP method that this binding is bound to. The HTTP method is only important to
     * routing when the Java method's path makes it the final method invoked in the
     * sequence that processes the HTTP message. Intermediate Java methods in the sequence
     * are always found using the GET HTTP method.
     *
     * <p>
     * When {@code method} is set to
     * {@link Method#Any}, the Java method acts as an accessor to the API on the return
     * type of the method. Such a Java method will be found when routing searches for a
     * GET method. After routing invokes the Java method and sets the return value's
     * component descriptor in the current scope, the next path search will ignore
     * class/interface-level {@code RequestMapping} paths.
     * </p>.
     *
     * <p>
     * A Java method whose @code RequestMapping} method is {@code Any} cannot be the final
     * method in the sequence.
     * </p>
     *
     * <p>
     * It is a runtime exception when the domain is loaded if a class/interface-level
     * {@code RequestMapping} defines a method element.
     * </p>
     *
     * @return the HTTP method that this binding is bound to
     */
    Method method() default Method.Any;

    boolean async() default false;

    /**
     * Whether to require that the caller include a CSRF token in the request header.
     * This should only be false for requests that the browser might make directly,
     * not via xhr. For example, attachment downloads.
     */
    boolean csrf() default true;

    /**
     * Identify the transaction mode for this request mapping.
     * Typically used to identify read-only search requests that require POST-ed search
     * criteria.
     */
    Mode mode() default Mode.DEFAULT;

    /**
     * Whether this request mapping method is always the last in a sequence.
     *
     * If true, any path segments following this method's will be ignored.
     */
    boolean terminal() default false;
}
