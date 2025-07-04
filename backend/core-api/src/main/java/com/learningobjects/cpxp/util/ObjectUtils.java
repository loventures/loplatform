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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

public class ObjectUtils extends org.apache.commons.lang3.ObjectUtils {
    /**
     * Compares two possibly null items with special support for numbers.
     */
    public static int compare(Object c0, Object c1) {
        if (c0 == null) {
            if (c1 != null) {
                return -1;
            } else {
                return 0;
            }
        } else {
            if (c1 == null) {
                return 1;
            } else if (c0 instanceof Number) { // to compare long vs int
                return compare(((Number) c0).longValue(), ((Number) c1).longValue());
            } else {
                return ((Comparable) c0).compareTo((Comparable) c1);
            }
        }
    }

    /**
     * Checks for item equality with special support for numbers.
     */
    public static boolean equals2(Object o0, Object o1) {
        if (o0 == null) {
            if (o1 != null) {
                return false;
            } else {
                return true;
            }
        } else {
            if (o1 == null) {
                return false;
            } else if (o0 instanceof Number) { // to compare long vs int
                return ((Number) o0).longValue() == ((Number) o1).longValue();
            } else {
                return o0.equals(o1);
            }
        }
    }

    public static int compare(long l0, long l1) {
        long delta = l0 - l1;
        return (delta < 0) ? -1 : (delta == 0) ? 0 : 1;
    }

    public static <T> T getFirstNonNullIn(T ... tArray) {
        return getFirstNonNullIn(Arrays.asList(tArray));
    }

    public static <T> T getFirstNonNullIn(Iterable<T> tIterable) {
        if (tIterable == null) {
            return null;
        }
        for (T t: tIterable) {
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public static int compareIgnoreCase(String s1, String s2) {
        if (s1 == null) {
            if (s2 != null) {
                return -1;
            } else {
                return 0;
            }
        } else {
            if (s2 == null) {
                return 1;
            } else {
                return s1.compareToIgnoreCase(s2);
            }
        }
    }

    /**
     * Convert an object to a string, swallowing exceptions.
     *
     * @param obj the object to stringify
     * @param dflt the value to return if stringification fails
     * @return obj.toString, or dflt
     */
    public static String safeToString(Object obj, Supplier<String> dflt) {
        try {
            return obj.toString();
        } catch (Exception e) {
            return dflt.get();
        }
    }
}
