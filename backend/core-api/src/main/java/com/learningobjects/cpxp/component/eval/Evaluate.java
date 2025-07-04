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

package com.learningobjects.cpxp.component.eval;

import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that describes how the component framework can generate values
 * for dependency injection. Any annotation that is annotated with {@link Evaluate}
 * can be used to annotate injected fields and parameters to describe how their value
 * should be computed.
 *
 * If `value` is not assigned, an evaluator will be picked
 * based on its {@link Evaluates} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
@Qualifier
public @interface Evaluate {
    /** The {@link Evaluator} class that should be used to compute a value for
     * dependency injection.
     */
    Class<? extends Evaluator> value() default Evaluator.class;
}
