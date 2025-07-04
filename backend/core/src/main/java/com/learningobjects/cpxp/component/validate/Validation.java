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

package com.learningobjects.cpxp.component.validate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an annotation as an RPC validation annotation.
 *
 * RPC parameters annotated with the target annotation will be
 * validated using the {@link Validator} specified by {@link #value}.
 *
 * Before the RPC method is invoked, the validator's {@link Validator#validate}
 * method will be called with the RPC target's instance, context, and the
 * supplied value for the annotated parameter. If it returns {@code null},
 * then the RPC proceeds as usual. If it returns a non-null {@code String},
 * then an error is returned to the client with the return value as the
 * error message.
 *
 * This is an RPC framework annotation and should not be used in new code.
 *
 * @see Validator
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.ANNOTATION_TYPE })
public @interface Validation {
    public Class<? extends Validator> value();
}
