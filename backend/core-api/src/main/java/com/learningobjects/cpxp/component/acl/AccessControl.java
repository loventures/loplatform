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

package com.learningobjects.cpxp.component.acl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that an annotation is an access controller for
 * RPC and Servlet components.
 *
 * The {@link AccessEnforcer#checkAccess()} of the class {@link #value()}
 * will be invoked in the context of the component when an @Rpc or @SiteView
 * annotated method is invoked in the on the annotated method or such a method
 * of the annotated class.
 *
 * A return value of false, or an exception, will cause the request to be
 * declined with a 403 Forbidden status.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface AccessControl {
    /** The class of the {@link AccessEnforcer} that will be invoked to check access privileges. */
    Class<? extends AccessEnforcer> value();
}
