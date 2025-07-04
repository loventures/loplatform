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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A named property comparator. For the property name "x", this orders
 * by the comparable value returned by the <tt>getX()</tt> method.
 * Multiple properties may be separated by commas; e.g. "x,y".
 */
public class PropertyComparator<T> implements Comparator<T> {
    /** the property name */
    private String _propertyName;

    /** ascending or descending order */
    protected boolean _ascending;

    /**
     * Create a new ascending property comparator.
     *
     * @param propertyName the property name (e.g. "x")
     */
    public PropertyComparator(String propertyName) {
        this(propertyName, true);
    }

    /**
     * Create a new property comparator.
     *
     * @param propertyName the property name (e.g. "x")
     * @param ascending whether to sort in ascending or descending order
     */
    public PropertyComparator(String propertyName, boolean ascending) {
        _propertyName = propertyName;
        _ascending = ascending;
    }

    /** the getter */
    private List<Method> _getters;

    /**
     * Gets the getters.
     */
    protected List<Method> getGetters(T a) throws Exception {
        if (_getters == null) {
            _getters = new ArrayList<Method>();
            for (String propertyName: _propertyName.split(",")) {
                _getters.add(a.getClass().getMethod("get" + StringUtils.capitalize(propertyName)));
            }
        }
        return _getters;
    }

    /**
     * {@inheritDoc}
     */
    public int compare(T a, T b) {
        try {
            for (Method getter: getGetters(a)) {
                Comparable c = (Comparable) getter.invoke(a);
                Comparable d = (Comparable) getter.invoke(b);
                int cmp = _ascending ? ObjectUtils.compare(c, d) : ObjectUtils.compare(d, c);
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
