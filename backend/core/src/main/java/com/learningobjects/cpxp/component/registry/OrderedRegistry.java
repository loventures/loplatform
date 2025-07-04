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

public class OrderedRegistry<T> extends BasicRegistry<T> {
    // Blah, google SortedMultimap doesn't have ceil/floor ability
    private final TreeMap<Comparable, List<T>> _registry = new TreeMap<>();
    private final List<T> _all = new ArrayList<>();

    @Override
    protected void put(Object key, T object) {
        List<T> list = _registry.computeIfAbsent((Comparable) key, k -> new ArrayList<>());
        list.add(object);
    }

    @Override
    protected T get(Object key) {
        Map.Entry<Comparable, List<T>> entry = _registry.ceilingEntry((Comparable) key);
        return (entry == null) ? null : entry.getValue().get(0);
    }

    @Override
    public Iterable<T> lookupAll() {
        synchronized (_all) {
            if (_all.isEmpty()) { // ugly, but...
                List<T> result = new ArrayList<>();
                for (List<T> list : _registry.values()) {
                    _all.addAll(list);
                }
            }
        }
        return _all;
    }

    @Override
    public Map<String, Collection<T>> toMap() {
        Map<String,Collection<T>> collectionMap = new HashMap<>();
        for(Map.Entry<Comparable,List<T>> entry : _registry.entrySet()) {
            String key = entry.getKey().getClass().getName();
            Collection previous = collectionMap.get(key);
            Collection<T> newCollection = null;
            if(previous == null) {
                newCollection = new ArrayList<>();
            } else {
                newCollection = previous;
            }
            newCollection.addAll(entry.getValue());
            collectionMap.put(key,newCollection);
        }
        return collectionMap;
    }
}
