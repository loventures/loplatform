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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MultiRegistry<T> extends BasicRegistry<T> {
    private final Multimap<Object, T> _registry = HashMultimap.create();

    @Override
    protected void put(Object key, T object) {
        _registry.put(key, object);
    }

    @Override
    protected T get(Object key) {
        return ObjectUtils.getFirstNonNullIn(_registry.get(key));
    }

    @Override
    public Iterable<T> lookupAll() {
        return _registry.values();
    }

    @Override
    public Map<String, Collection<T>> toMap() {
        Map<String,Collection<T>> collectionMap = new HashMap<>();
        for(Map.Entry<Object,T> entry : _registry.entries()) {
            String key = entry.getKey() == null ? "<null>" : entry.getKey().toString();
            Collection previous = collectionMap.get(key);
            Collection<T> newCollection = null;
            if(previous == null) {
                newCollection = new ArrayList<>();
            } else {
                newCollection = previous;
            }
            newCollection.add(entry.getValue());
            collectionMap.put(key,newCollection);
        }
        return collectionMap;
    }
}
