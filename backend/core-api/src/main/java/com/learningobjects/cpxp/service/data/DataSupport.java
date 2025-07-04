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

package com.learningobjects.cpxp.service.data;

import java.util.Date;

public class DataSupport {
    /** Arbitrary minimum time. */
    public static final Date MIN_TIME = new Date(-1891382400000L); // 1910
    /** Arbitrary maximum time. */
    public static final Date MAX_TIME = new Date(4415817600000L); // 2109


    public static boolean isMinimal(Date date) {
        return (date == null) || (date.compareTo(MIN_TIME) <= 0);
    }

    public static boolean isMaximal(Date date) {
        return (date == null) || (date.compareTo(MAX_TIME) >= 0);
    }

    public static Date defaultToMinimal(Date date) {
        return isMinimal(date) ? MIN_TIME : date;
    }

    public static Date minimalToNull(Date date) {
        return isMinimal(date) ? null : date;
    }

    public static Date defaultToMaximal(Date date) {
        return isMaximal(date) ? MAX_TIME : date;
    }

    public static Date maximalToNull(Date date) {
        return isMaximal(date) ? null : date;
    }

    /**
     * Convert a number to a date.
     *
     * @param number the number
     *
     * @return the date
     */
    public static Date toTime(final Long number) {
        if (number == null) {
            return null;
        } else {
            return new Date(number.longValue());
        }
    }

    /**
     * Convert a date to a number.
     *
     * @param date the date
     *
     * @return the number
     */
    public static Long toNumber(final Date date) {
        if (date == null) {
            return null;
        } else {
            return Long.valueOf(date.getTime());
        }
    }

    /**
     * Convert a number to a boolean.
     *
     * @param number the number
     *
     * @return the boolean
     */
    public static Boolean toBoolean(final Long number) {
        if (number == null) {
            return null;
        } else {
            return number.longValue() > 0L;
        }
    }

    /**
     * Convert a boolean to a number.
     *
     * @param bool the boolean
     *
     * @return the number
     */
    public static Long toNumber(final Boolean bool) {
        if (bool == null) {
            return null;
        } else {
            return bool.booleanValue() ? Long.valueOf(1) : Long.valueOf(0);
        }
    }

}
