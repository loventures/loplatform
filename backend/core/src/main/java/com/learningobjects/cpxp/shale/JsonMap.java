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

package com.learningobjects.cpxp.shale;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A typedef for a map from strings to objects.
 */
public class JsonMap extends HashMap<String, Object> {
    public JsonMap(final Map<String, Object> source) {
        super(source);
    }

    public JsonMap() {
        super(4);
    }

    public JsonMap add(String key, Object value) {
        put(key, value);
        return this;
    }

    public JsonMap putIfPresent(String key, Optional<?> optValue) {
        optValue.ifPresent(value -> put(key, value));
        return this;
    }

    public JsonMap putIfNotEmpty(String key, Collection<? extends Object> values) {
        if (values != null && !values.isEmpty()) {
            put(key,values);
        }
        return this;
    }

    public JsonMap add(JsonMap map) {
        this.putAll(map);
        return this;
    }

    public static JsonMap of(String key, Object value) {
        JsonMap map = new JsonMap();
        map.put(key, value);
        return map;
    }

    public static JsonMap of(String key0, Object value0, String key1, Object value1) {
        JsonMap map = new JsonMap();
        map.put(key0, value0);
        map.put(key1, value1);
        return map;
    }

}
