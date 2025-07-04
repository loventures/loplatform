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

import java.lang.reflect.Method;

/**
 * A caseless named property comparator. For the property name "x", this orders
 * by the comparable value returned by the <tt>getX()</tt> method.
 * Multiple properties may be separated by commas; e.g. "x,y".
 */
public class PropertyIgnoreCaseComparator<T> extends PropertyComparator<T> {
    /**
     * Create a new property comparator.
     *
     * @param propertyName the property name (e.g. "x")
     * @param ascending whether to sort in ascending or descending order
     */
    public PropertyIgnoreCaseComparator(String propertyName, boolean ascending) {
        super(propertyName, ascending);
    }

    /**
     * {@inheritDoc}
     */
    public int compare(T a, T b) {
        try {
            for (Method getter: getGetters(a)) {
                String c = (a == null) ? null : (String) getter.invoke(a);
                String d = (b == null) ? null : (String) getter.invoke(b);
                int cmp = _ascending ? ObjectUtils.compareIgnoreCase(c, d) : ObjectUtils.compareIgnoreCase(d, c);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        } catch (Exception ex) {
            throw new RuntimeException("Comparison error", ex);
        }
    }
}
