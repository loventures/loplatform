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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class PathRegistry<T> extends BasicRegistry<T> {
    private final TreeMap<String, T> _registry = new TreeMap<>();

    @Override
    protected void put(Object key, T object) {
        _registry.put((String) key, object);
    }

    @Override
    protected T get(Object key) {
        String str = (String) key; // /foo/bar/baz
        int i = str.indexOf('/', 1);
        String base = str.substring(0, (i < 0) ? str.length() : i); // /foo
        for (Map.Entry<String, T> entry : _registry.subMap(base, true, str, true).descendingMap().entrySet()) {
            if (entry.getKey().equals(key) || str.startsWith(entry.getKey() + '/')) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public Iterable<T> lookupAll() {
        return _registry.values();
    }

    @Override
    public Map<String, Collection<T>> toMap() {
        Map<String,Collection<T>> collectionMap = new TreeMap<>();
        for(Map.Entry<String,T> entry : _registry.entrySet()) {
            collectionMap.put(entry.getKey(), Collections.singleton(entry.getValue()));
        }
        return collectionMap;
    }
}
