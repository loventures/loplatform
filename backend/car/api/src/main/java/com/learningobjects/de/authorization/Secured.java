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

package com.learningobjects.de.authorization;

import loi.cp.right.Right;
import loi.cp.right.RightMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Secures a method or type by denying its invocation if the subject is not granted
 * with the authorities described by this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Secured {

    /**
     * Permits invocation if the subject has collected at least one of these rights. If
     * {@link #conjunction()} is true, then the subject must have all of these rights.
     * Invocation is permitted to all subjects if this array is empty and {@link
     * #byOwner()} is false.
     */
    Class<? extends Right>[] value() default {};

    /** What match applies to these rights. */
    RightMatch match() default RightMatch.EXACT;

    /**
     * Permits invocation if the subject is the owner that is collected in the {@link
     * SecurityContext}
     */
    boolean byOwner() default false;

    /**
     * If true, all authorities described by this annotation must be collected. If false,
     * at least one must be collected.
     */
    boolean conjunction() default false;

    Class<? extends SecurityGuard>[] guard() default {};

    /**
     * Whether or not the anonymous user will be allowed to take part in the security
     * checks declared by this annotation. By default the anonymous user is rejected
     * immediately (guards will not even be invoked).
     */
    boolean allowAnonymous() default false;

    /**
     * On a method-level request mapping, whether this @Secured should override any
     * type-level @Secured rather than adding to it.
     */
    boolean overrides() default false;
}
