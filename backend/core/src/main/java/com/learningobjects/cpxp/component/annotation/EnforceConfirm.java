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
import com.learningobjects.cpxp.component.acl.ConfirmEnforcer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the user agent to make two requests to this RPC
 * in order to invoke the annotated method.
 *
 * The first request will result in an HTTP 202, and the
 * request should be repeated, passing the query parameter
 * {@code ug:confirmed=true}.
 *
 * This is taken care of automatically by {@code ug.rpc}.
 *
 * @see AccessControl
 * @see ConfirmEnforcer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD })
@AccessControl(ConfirmEnforcer.class)
public @interface EnforceConfirm {
    /** A custom title to apply to any message box asking for confirmation that will be shown to the user. */
    @MessageValue
    String title() default "";

    /** A custom message to display to the user when asking for confirmation. */
    @MessageValue
    String message() default "";
}
