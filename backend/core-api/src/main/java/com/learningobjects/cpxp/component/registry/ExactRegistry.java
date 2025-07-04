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

import java.util.*;

public class ExactRegistry<T> extends BasicRegistry<T> {
    private final Map<Object, T> _registry = new HashMap<>();

    @Override
    protected void put(Object key, T object) {
        _registry.put(key, object);
    }

    @Override
    protected T get(Object key) {
        return _registry.get(key);
    }

    @Override
    public Iterable<T> lookupAll() {
        return _registry.values();
    }

    @Override
    public Map<String, Collection<T>> toMap() {
        Map<String,Collection<T>> map = new HashMap<>();
        for(Map.Entry<Object,T> entry : _registry.entrySet()) {
            map.put(entry.getKey().toString(),Collections.singleton(entry.getValue()));
        }
        return map;
    }
}
