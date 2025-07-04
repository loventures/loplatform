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

package com.learningobjects.cpxp.util;

/**
 * An ordered multikey.
 */
public class MultiKey extends org.apache.commons.collections4.keyvalue.MultiKey<Object> implements Comparable<MultiKey> {
    public MultiKey(Object... o) {
        super(o);
    }

    public int compareTo(MultiKey mk) {
        int n = Math.min(size(), mk.size()), cmp = 0;
        for (int i = 0; (cmp == 0) && (i < n); ++ i) {
            cmp = ObjectUtils.compare((Comparable) getKey(i), (Comparable) mk.getKey(i));
        }
        if (cmp == 0) {
            cmp = size() - mk.size();
        }
        return cmp;
    }
}
