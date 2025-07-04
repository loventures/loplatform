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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on stateless services that need to be dependency injected.
 * Can be used in three forms,
 *
 * If used to annotate a class, it indicates that that class is a stateless service that needs to be
 * injected into other classes, and the annotation's attributes define behaviors of the DI.  The @Service annotated
 * class will have its own members injected into it on creation.
 *
 * If used on an interface, it indicates that its implementation will be an @Service-annotated class.  Attributes are
 * not used in this case.
 *
 * If used to annotate a member, it indicates that this member needs to be created through dependency injection and
 * will search for a class annotated with @Service of the desired type.  Attributes are not used in this case.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Evaluate
// @Bound(ServiceBinding.class) // effectively...
public @interface Service {
    /** Other components that must be bootstrapped first. */
    Class<?>[] dependencies() default {};

    /** Other services that this suppresses. */
    Class<?>[] suppresses() default {};

    /** Indicates default enabled */
    boolean enabled() default true;

    /** Whether just one instance should exist in the whole JVM. Really singleton. */
    boolean unique() default false;
}
