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
import java.util.Collections;
import java.util.Map;

public class SingletonRegistry<T> implements Registry<Annotation, T> {
    private T _object;

    @Override
    public void register(Annotation annotation, T object) {
        _object = object;
    }

    @Override
    public T lookup(Object[] keys) {
        return _object;
    }

    @Override
    public Iterable<T> lookupAll() {
        if (_object == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(_object);
    }

    @Override
    public Map<String, Collection<T>> toMap() {
        return Collections.singletonMap("*",Collections.singleton(_object));
    }
}
