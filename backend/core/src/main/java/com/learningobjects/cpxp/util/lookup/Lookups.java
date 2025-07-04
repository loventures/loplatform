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

package com.learningobjects.cpxp.util.lookup;

import com.google.common.base.Predicate;

import java.util.*;
import java.util.Map.Entry;

public class Lookups {

    public static <K,V> Lookup<K,V> empty() {
        return lookup(Collections.<K,V>emptyMap());
    }

    public static <K, V> Lookup<K, V> lookup(final Map<K, V> map) {
        Objects.requireNonNull(map);

        class MapAdapter implements Lookup<K, V> {
            @Override
            public Optional<V> get(K key) {
                return Optional.ofNullable(map.get(key));
            }

            @Override
            public Collection<K> keySet() {
                return Collections.unmodifiableCollection(map.keySet());
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return Collections.unmodifiableSet(map.entrySet()).iterator();
            }
        }

        return new MapAdapter();
    }

    public static <K, V> Lookup<K, V> filter(Lookup<K, V> lookup, Predicate<K> filter) {
        Collection<K> keys = lookup.keySet();
        if (keys.isEmpty()) return empty();

        Map<K, V> filteredMap = new HashMap<>();
        for (K key : keys) {
            if (filter.apply(key)) {
                filteredMap.put(key, lookup.get(key).orElse(null));
            }
        }

        return lookup(filteredMap);
    }

}
