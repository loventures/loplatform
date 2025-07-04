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

package com.learningobjects.cpxp.component.registry;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;

import java.lang.annotation.*;

/**
 * Declares that a registry be made in the component environment that indexes all {@link
 * Component} (and {@link Service}?) types that are subtypes of the {@link Bound} target.
 *
 * <p>Should be called 'Binds' or 'Binder', so that the term 'bound' could refer to the
 * subtype being bound. Also note that it is the subtype's {@link DelegateDescriptor} that
 * is registered, not the {@link Class} itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Bound {

    /**
     * The binding annotation type whose declaration on a subtype of the {@link Bound}
     * target provides details for the insertion and retrieval of the subtype in the
     * registry. This type must itself be annotated with {@link Binding}.
     *
     * @return the binding annotation type
     */
    Class<? extends Annotation> value();
}
