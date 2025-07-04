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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

public interface Registry<A extends Annotation, T> {
    default void init(ComponentRegistry owner) {}

    public void register(A annotation, T object);
    public T lookup(Object[] keys);
    public Iterable<T> lookupAll();

    /**
     * This method is meant to return a map for conceptual introspection of registry state, do not rely on this.
     * @return A Map representing the state of the registry.
     */
    public Map<String,Collection<T>> toMap();
}
