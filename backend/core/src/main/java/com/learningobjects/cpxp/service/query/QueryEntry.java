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

package com.learningobjects.cpxp.service.query;

import com.learningobjects.cpxp.util.cache.Entry;

import java.util.HashSet;
import java.util.Set;

public class QueryEntry extends Entry<String, QueryResults> {

    protected QueryEntry(QueryResults results, String key) {
        super(key, results, invalidationKeys(results));
    }

    private static Set<String> invalidationKeys(QueryResults results) {
        Set<String> keys = new HashSet<>();
        if (results.getInvalidationKeys() != null) {
            keys.addAll(results.getInvalidationKeys());
        }
        if (results.getParent() != null) {
            keys.add(results.getParent().toString());
            if (results.getItemType() != null) {
                String parentKey = results.getParent() + "/" + results.getItemType();
                keys.add(parentKey);
            }
        }
        return keys;
    }
}
