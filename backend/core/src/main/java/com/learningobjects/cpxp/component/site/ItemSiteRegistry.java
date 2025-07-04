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

package com.learningobjects.cpxp.component.site;

import com.learningobjects.cpxp.component.registry.Registry;
import com.learningobjects.cpxp.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ItemSiteRegistry<T> implements Registry<ItemSiteBinding, T> {
    private final Map<String, T> _registry = new HashMap<>();

    @Override
    public void register(ItemSiteBinding binding, T object) {
        for (String type : binding.type()) {
            if (binding.subtype().length == 0) {
                _registry.put(type + '!' + binding.action(), object);
            } else {
                for (String subtype : binding.subtype()) {
                    _registry.put(type + '/' + subtype + '!' + binding.action(), object);
                }
            }
        }
    }

    @Override
    public T lookup(Object[] keys) {
        String type = (String) keys[0], subtype = (String) keys[1], action = StringUtils.defaultString((String) keys[2], "view");
        T object = null;
        if (subtype != null) {
            object = _registry.get(type + '/' + subtype + '!' + action);
            if (object == null) {
                object = _registry.get(type + '/' + subtype + "!*");
            }
        }
        if (object == null) {
            object = _registry.get(type + '!' + action);
            if (object == null) {
                object = _registry.get(type + "!*");
            }
        }
        return object;
    }

    @Override
    public Iterable<T> lookupAll() {
        return _registry.values();
    }

    @Override
    public Map<String, Collection<T>> toMap() {
        Map<String,Collection<T>> collectionMap = new HashMap<>();
        for(Map.Entry<String,T> entry : _registry.entrySet()) {
            collectionMap.put(entry.getKey(), Collections.singleton(entry.getValue()));
        }
        return collectionMap;
    }
}

