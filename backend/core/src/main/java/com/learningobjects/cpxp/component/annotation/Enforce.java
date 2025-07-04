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

import com.learningobjects.cpxp.component.acl.AccessControl;
import com.learningobjects.cpxp.component.acl.MethodEnforcer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces a custom access control policy on this RPC.
 *
 * The named methods (using MethodRef syntax {@code "#methodName"})
 * will be invoked in the context of this component.
 *
 * Only if all of the methods return true will the RPC be invoked.
 *
 * @see AccessControl
 * @see MethodEnforcer
 * @see MethodRef
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE })
@AccessControl(MethodEnforcer.class)
public @interface Enforce {
    /**
     * The methods to use to check access privileges.
     * Each one should be of the form {@code "#methodName"} where
     * methodName is the name of a method on the component class or one
     * of its delegates, annotated with {@link Fn}.
     */
    String[] value();
}
