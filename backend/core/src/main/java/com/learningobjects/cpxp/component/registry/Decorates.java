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

import com.learningobjects.cpxp.component.ComponentInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to denote what component **implementation** a polymorphic decorator
 * implementation should be bound to.
 *
 * Prefer binding to implementations over interfaces.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
@Binding(property = "value", registry = ClassKeyRegistry.class) // should this be a ClassRegistry (instanceof)?
public @interface Decorates {
    public Class<? extends ComponentInterface>[] value();
}
