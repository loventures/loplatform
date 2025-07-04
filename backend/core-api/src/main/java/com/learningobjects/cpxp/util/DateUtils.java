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

import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils extends org.apache.commons.lang3.time.DateUtils {

    public static final double DELTA_OVERRUN = 1.0;

    public static enum Unit {
        millisecond(1L, "ms", Calendar.MILLISECOND),
        second(1000L, "s", Calendar.SECOND),
        minute(60L * 1000, "m", Calendar.MINUTE),
        hour(60L * 60 * 1000, "h", Calendar.HOUR),
        day(24L * 60 * 60 * 1000, "d", Calendar.DAY_OF_YEAR),
        week(7L * 24 * 60 * 60 * 1000, "w", Calendar.WEEK_OF_YEAR),
        month(30L * 24 * 60 * 60 * 1000, "M", Calendar.MONTH), // approx
        year(365L * 24 * 60 * 60 * 1000, "y", Calendar.YEAR); // approx

        private final long _ms;
        private final String _suffix;
        private final int _field;

        Unit(long ms, String suffix, int field) {
            _ms = ms;
            _suffix = suffix;
            _field = field;
        }

        public long getValue() {
            return _ms;
        }

        public long getValue(double amount) {
            return (long) (_ms * amount);
        }

        public int getCalendarField() {
            return _field;
        }

        public String getSuffix() {
            return _suffix;
        }
    }

    public static class Delta {
        private Unit _unit;
        private double _scaled;
        private boolean _negative;

        public Delta(Unit unit, double scaled, boolean negative) {
            _unit = unit;
            _scaled = scaled;
            _negative = negative;
        }

        public Unit getUnit() {
            return _unit;
        }

        public double getScaled() {
            return _scaled;
        }

        public boolean isNegative() {
            return _negative;
        }

        public String toString() {
            long n = (long) Math.round(getScaled());
            return (_negative ? "-" : "") + n + " " + _unit + (n == 1 ? "" : "s");
        }
    }

    public static Date getQuantumOlder(Date date) {
        return (date == null) ? null : new Date(date.getTime() - 1L);
    }

    public static Date getQuantumNewer(Date date) {
        return (date == null) ? null : new Date(date.getTime() + 1L);
    }

    // at 1.0 overrun, will switch immediately to the next unit up.
    // at 1.5 overrun, will switch to the next unit up when it
    // reaches 1.5 of those units; the switches to months and years
    // are approximate.
    public static Unit getDurationUnit(long ms, double overrun) {
        Unit unit = Unit.millisecond;
        for (Unit u: Unit.values()) {
            if ((u.getValue() > 0) && (ms >= u.getValue() * overrun)) {
                unit = u;
            }
        }
        return unit;
    }

    public static Delta getDelta(Date date, Date base) {
        long ms = date.getTime() - base.getTime();
        boolean negative = (ms < 0);
        ms = negative ? -ms : ms;
        Unit unit = getDurationUnit(ms, DELTA_OVERRUN);
        double scaled = (double) ms / unit.getValue();
        return new Delta(unit, scaled, negative);
    }

    public static Date parseTime(String time, String pattern) throws ParseException {
        SimpleDateFormat fmt = new SimpleDateFormat(pattern);
        return fmt.parse(time);
    }

    public static String formatDuration(Long value) {
        if (value == null) {
            return "";
        }

        Unit unit = getDurationUnit(value, 1.5);
        double scaled = NumberUtils.divide(value.longValue(), unit.getValue(), 0);
        return ((long) scaled) + " " + unit + "s"; // ugh
    }

    public static String formatDurationPrecisely(Long value) {
        List<String> list = formatDurationPreciselyHelper(value);

        if (null == list || list.size() < 1) {
            return "";
        } else if (list.size() == 1) {
            return list.get(0);
        } else if (list.size() == 2) {
            return list.get(0)+ " and "+list.get(1);
        } else {

            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < list.size() -1 ; i++) {
                sb.append(list.get(i));
                sb.append(", ");
            }

            sb.append("and ");
            sb.append(list.get(list.size() - 1));

            return sb.toString();
        }

    }

    private static List<String> formatDurationPreciselyHelper(Long value) {
        ArrayList<String> formattedParts = new ArrayList<String>();

        if (value == null) {
            return formattedParts;
        }

        Unit unit = getDurationUnit(value, 1.0);
        long scaled = value / unit.getValue();

        String firstString = ((long) scaled) + " " + unit + (scaled > 1 ? "s" : ""); // ugh
        formattedParts.add(firstString);

        long remainder = value.longValue() - ((long)(scaled*unit.getValue()));
        if (remainder != 0) {
            formattedParts.addAll(formatDurationPreciselyHelper(remainder));
        }

        return formattedParts;
    }

    /**
     * Parses a duraton and a unit back into a millisecond value.
     *
     * @param value
     *            string with an integer portion and a unit specifier
     * @return an integer value representing the corresponding number of milliseconds,
     *         null if the input is null or empty
     */
    public static Long parseDuration(String value) {
        Delta delta = parseDelta(value);
        if (delta == null) {
            return null;
        }
        return delta.getUnit().getValue(delta.getScaled());
    }

    public static Delta parseDelta(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "(-?[0-9]+\\.?[0-9]*)\\s*(\\S*)");
        Matcher matcher = pattern.matcher(value);
        double numeral = 0.0;
        Unit unit = null;
        if (matcher.matches()) {
            numeral = Double.parseDouble(matcher.group(1));
            String unitName = matcher.group(2);
            if (StringUtils.isEmpty(unitName)) {
                unit = Unit.millisecond;
            } else {
                for (Unit unitEnum : Unit.values()) {
                    if (StringUtils.startsWithIgnoreCase(unitName, unitEnum.name()) || unitName.equals(unitEnum.getSuffix())) {
                        unit = unitEnum;
                        break;
                    }
                }
            }
        }
        if (null == unit) {
            throw new IllegalArgumentException(String.format(
                    "Couldn't parse input, %1$s.", value));
        }
        return new Delta(unit, numeral, false);
    }

    public static Date delta(long ms) {
        return new Date(System.currentTimeMillis() + ms);
    }

    /**
     * Converts from milliseconds from epoch into a date with utc timezone
     * @param ms
     * @return
     */
    public static Date convertToUtc(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(ms);
        return cal.getTime();
    }

    public static Date delta(Date ref, long ms) {
        return new Date(ref.getTime() + ms);
    }

    public static Date getDateWithoutTime(Date date) {
        return org.apache.commons.lang3.time.DateUtils.truncate(date,Calendar.DAY_OF_MONTH);
    }

    /**
     * Returns whether it is currently past the indicated deadline
     * @param deadline the deadline; null means no deadline, thus, return false
     * @return whether the deadline has passed
     */
    public static boolean hasDeadlinePassed(@Nullable Date deadline) {
        return deadline != null && new Date().after(deadline);
    }


    // We approximate the time for some operations so that caching can work
    // better.
    private static final long TIME_APPROXIMATION = 60 * 1000L; // 1 minute

    public static Date getApproximateTime(Date date) {
        if (date == null) return null;
        long ms = date.getTime();
        return new Date(ms - (ms % TIME_APPROXIMATION));
    }
    public static Date getApproximateTimeCeiling(Date date) {
        if (date == null) return null;
        long ms = date.getTime(), approx = ms % TIME_APPROXIMATION;
        return new Date(ms + (approx > 0 ? TIME_APPROXIMATION - approx : 0));
    }

}
