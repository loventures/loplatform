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

import com.google.common.base.Function;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Number utils.
 */
public class NumberUtils extends org.apache.commons.lang3.math.NumberUtils {
    public static final Long ZERO_LONG = Long.valueOf(0L);

    /**
     * A {@link Function} for {@link Number#longValue()}
     */
    public static final Function<Number, Long> LONG_VALUE = new Function<Number, Long>() {

        @Nullable
        @Override
        public Long apply(@Nullable final Number number) {
            return number == null ? null : number.longValue();
        }
    };

    public static enum Unit {
        bytes, KB, MB, GB, TB, PB, EB; // ZB, YB beyond long

        public long getValue() {
            return getValue(1L);
        }

        public long getValue(double amt) {
            return (long) (amt * (1L << (10 * ordinal()))); // 1<<10 = 1KB, 1<<20 = 1MB, etc
        }

        /**
         * Finds the maximum unit whose value divides completely into the provided
         * value.
         *
         * @param value
         *            value to check, may be null, will convert to zero if null
         * @return the largest unit that can be used to used to format the provided
         *         value
         */
        public static Unit maximum(Long value) {
            long toFind = longValue(value);
            if (toFind == 0) {
                toFind = 1;
            } else if (toFind < 0) {
                toFind = -toFind;
            }
            int log2 = log2(toFind);
            int unitIndex = log2 / 10;
            if (unitIndex >= Unit.values().length) {
                unitIndex = Unit.values().length - 1;
            }
            return Unit.values()[unitIndex];
        }

        private static int log2(long num) {
            return 63 - Long.numberOfLeadingZeros(num);
        }

    }

    public static long maxIfNull(Long x) {
        return (x == null) ? Long.MAX_VALUE : x.longValue();
    }

    public static String formatDataSize(long value) {
        return formatDataSize(value, null, 1, true);
    }

    public static Unit getDataSizeUnit(long value, int places) {
        if (value < 0) {
            value = -value;
        }
        // pick the unit that lets the scaled value be at least places ? 1 : 4
        Unit unit = Unit.bytes;
        for (Unit u : Unit.values()) {
            if ((value >= u.getValue() * ((places > 0) ? 1 : 4))) {
                unit = u;
            }
        }
        return unit;
    }

    public static String formatDataSize(Long value, Unit unit, int places,
            boolean includeUnit) {
        if (value == null) {
            return "";
        }

        if (unit == null) {
            unit = getDataSizeUnit(value, places);
        }

        double scaled = divide(value, unit.getValue(), places);

        String string = NumberUtils.format(scaled, places);
        if (includeUnit) {
            string = string + ' ' + unit;
        }

        return string;
    }

    public static String format(double val, int digits) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(digits);
        return nf.format(val);
    }

    // rounds up
    public static double divide(long a, long b, int scale) {
        if (b == 0) {
            return 0.0;
        }
        BigDecimal dividend = BigDecimal.valueOf(a);
        BigDecimal divisor = BigDecimal.valueOf(b);
        BigDecimal result = dividend.divide(divisor, scale,
                BigDecimal.ROUND_HALF_UP);
        return result.doubleValue();
    }

    // rounds up, less expensive than the above
    public static int percent(long a, long b) {
        return (b == 0) ? 0 : (int) ((100L * a + b / 2) / b);
    }

    public static final String BASE_62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final char[] BASE_62_CHARS = BASE_62.toCharArray();

    public static String getBase62String(int chars) {
        // This has a very imperfect distribution
        BigInteger modulus = BigInteger.valueOf(62).pow(chars);
        byte[] bytes = new byte[(modulus.bitLength() + 7) >> 3];
        __rng.nextBytes(bytes);
        BigInteger value = new BigInteger(1, bytes);
        String str = NumberUtils.toBase62Encoding(value.mod(modulus));
        return StringUtils.repeat("0", chars - str.length()) + str;
    }

    /**
     * Encodes a number into the characters 0-9, a-z and A-Z.
     */
    public static String toBase62Encoding(BigInteger number) {
        return toBaseEncoding(number, BASE_62_CHARS, 62);
    }

    public static BigInteger fromBase62Encoding(String message) {
        BigInteger number = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(62);
        char[] chars = message.toCharArray();
        for (int i = chars.length - 1; i >= 0; -- i) {
            char chr = chars[i];
            int value;
            if ((chr >= '0') &&  (chr <= '9')) {
                value = chr - '0';
            } else if ((chr >= 'a') && (chr <= 'z')) {
                value = 10 + chr - 'a';
            } else if ((chr >= 'A') && (chr <= 'Z')) {
                value = 36 + chr - 'A';
            } else {
                throw new NumberFormatException(message);
            }
            number = number.multiply(base).add(BigInteger.valueOf(value));
        }
        return number;
    }

    public static final String BASE_48 = "23456789bcdfghjklmnpqrstvwxzBCDFGHJKLMNPQRSTVWXZ";
    private static final char[] BASE_48_CHARS = BASE_48.toCharArray();

    /**
     * Encodes a number into alphanumeric characters excluding vowels, 0, 1 and y, in an effort to reduce the chance
     * of a real word emerging.
     */
    public static String toBase48Encoding(BigInteger number) {
        return toBaseEncoding(number, BASE_48_CHARS, 48);
    }

    public static BigInteger fromBase48Encoding(String message) {
        return fromBaseEncoding(message, BASE_48);
    }

    private static BigInteger fromBaseEncoding(String message, String alphabet) {
        BigInteger number = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(alphabet.length());
        char[] chars = message.toCharArray();
        for (int i = chars.length - 1; i >= 0; -- i) {
            char chr = chars[i];
            int value = alphabet.indexOf(chr);
            if (value < 0) {
                throw new NumberFormatException(message);
            }
            number = number.multiply(base).add(BigInteger.valueOf(value));
        }
        return number;
    }

    /**
     * Encodes a number into the characters 0-9, a-z. This is case insensitive.
     */
    public static String toBase36Encoding(BigInteger number) {
        return toBaseEncoding(number, BASE_62_CHARS, 36);
    }

    public static BigInteger fromBase36Encoding(String message) {
        BigInteger number = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(36);
        char[] chars = message.toCharArray();
        for (int i = chars.length - 1; i >= 0; -- i) {
            char chr = chars[i];
            int value = (chr <= '9') ? (chr - '0') : (10 + chr - 'a');
            number = number.multiply(base).add(BigInteger.valueOf(value));
        }
        return number;
    }

    public static final String BASE_31 = "23456789abcdefghjkmnpqrstuvwxyz";
    private static final char[] BASE_31_CHARS = BASE_31.toCharArray();

    /**
     * Encodes a number into the characters 2-9, a-hj-km-np-z. This is case insensitive and avoids numbers and digits that might be confused.
     */
    public static String toBase31Encoding(BigInteger number) {
        return toBaseEncoding(number, BASE_31_CHARS, 31);
    }

    public static BigInteger fromBase31Encoding(String string) {
        return fromBaseEncoding(string, BASE_31);
    }

    public static final String BASE_26 = "abcdefghijklmnopqrstuvwxyz";
    private static final char[] BASE_26_CHARS = BASE_26.toCharArray();

    /**
     * Returns a random alpha id of the form bcdfghjk
     */
    public static String alphaRandom() { // Entropy: (26**7*25)
        BigInteger rnd = BigInteger.valueOf(8031810176L + Math.abs(__rng.nextLong() % 200795254400L));
        return toBaseEncoding(rnd, BASE_26_CHARS, 26);
    }

    public static BigInteger fromBase26Encoding(String string) {
        BigInteger number = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(26);
        char[] chars = string.toCharArray();
        for (int i = chars.length - 1; i >= 0; --i) {
            char chr = chars[i];
            int value = (chr <= '9') ? (chr - '0') : (chr - 'a');
            number = number.multiply(base).add(BigInteger.valueOf(value));
        }
        return number;
    }

    public static final String BASE_20 = "bcdfghjklmnpqrstvwxz";
    private static final char[] BASE_20_CHARS = BASE_20.toCharArray();
    private static final SecureRandom __rng = new SecureRandom();

    public static final SecureRandom getSecureRandom() {
        return __rng;
    }

    public static byte[] getNonce() {
        return getNonce(16);
    }

    public static byte[] getNonce(int len) {
        byte[] nonce = new byte[len];
        NumberUtils.getSecureRandom().nextBytes(nonce);
        return nonce;
    }

    /**
     * Returns a random id of the form Bcdfgh.1234.
     */
    public static String generateId() { // Entropy: (20**6)*(10**4) =~ 39 bits = 640000000000... Sqrt is 800000.
        BigInteger maskedId = BigInteger.valueOf(3200000L + __rng.nextInt(60800000));
        return StringUtils.capitalize(toBase20Encoding(maskedId)) + '.' + (1000 + __rng.nextInt(9000));
    }

    /**
     * Encodes a number into the characters a-z less aeiouy.
     */
    public static String toBase20Encoding(BigInteger number) {
        return toBaseEncoding(number, BASE_20_CHARS, 20);
    }

    private static String toBaseEncoding(BigInteger number, char[] chars, int n) {
        final BigInteger base = BigInteger.valueOf(n);
        final StringBuilder result = new StringBuilder();
        do {
            BigInteger[] divMod = number.divideAndRemainder(base);
            number = divMod[0];
            result.append(chars[divMod[1].intValue()]);
        } while (!BigInteger.ZERO.equals(number));
        return result.toString();
    }

    /**
     * Get the primitive long value of a Number, handling null.
     *
     * @param value
     *            the Number
     *
     * @return the primitive long value, or 0
     */
    public static long longValue(Number value) {
        return (value == null) ? 0L : value.longValue();
    }

    public static double doubleValue(Number value) {
        return (value == null) ? 0.0 : value.doubleValue();
    }

    /**
     * Get the primitive int value of a Number, handling null.
     *
     * @param value
     *            the Number
     *
     * @return the primitive int value, or 0
     */
    public static int intValue(Number value) {
        return (value == null) ? 0 : value.intValue();
    }

    public static int intValue(Number value, int otherwise) {
        return (value == null) ? otherwise : value.intValue();
    }

    /**
     * Synonym for {@link NumberUtils#parseLong(String)} that calls
     * {@link String#toString()} on the argue prior to parsing (convenient for
     * values returned from session objects, and the like).
     *
     * @param value
     *            Any {@link Object} whose {@link String#toString()} result may
     *            be parsed with {@link Long#valueOf(String)}.
     *
     * @return The parsed {@link Long} or {@code null} if the argument is
     *         {@code null} or empty.
     */
    public static Long parseLong(@Nullable final Object value) {
        return (null == value) ? null : parseLong(StringUtils.trimToNull(value.toString()));
    }

    /**
     * Parse a string to a long with null (or "null") safety.
     *
     * @param string the string
     *
     * @return the parsed Long, or null
     */
    public static Long parseLong(@Nullable String string) {
        return (StringUtils.isEmpty(string) || "null".equals(string)) ? null : Long.valueOf(string);
    }

    /**
     * Synonym for {@link NumberUtils#parseDouble(String)} that calls
     * {@link String#toString()} on the argue prior to parsing (convenient for
     * values returned from session objects, and the like).
     *
     * @param value
     *            Any {@link Object} whose {@link String#toString()} result may
     *            be parsed with {@link Double#valueOf(String)}.
     *
     * @return The parsed {@link Double} or {@code null} if the argument is
     *         {@code null} or empty.
     */
    public static Double parseDouble(@Nullable final Object value) {
        return (null == value) ? null : parseDouble(StringUtils.trimToNull(value.toString()));
    }

    /**
     * Parse a string to a double with null (or "null") safety.
     *
     * @param string the string
     *
     * @return the parsed Double, or null
     */
    public static Double parseDouble(@Nullable String string) {
        return (StringUtils.isEmpty(string) || "null".equals(string)) ? null : Double.valueOf(string);
    }

    /**
     * Parse a string to a long with null (or "null") safety.
     *
     * @param string the string
     *
     * @return the parsed Long, or null
     */
    public static Integer parseInteger(@Nullable String string) {
        return ((string == null) || "null".equals(string)) ? null : Integer.valueOf(string);
    }

    /**
     * Parses a numeral and a unit back into an integer value.
     *
     * @param value
     *            string with an integer portion and a unit specifier
     * @return an integer value representing the corresponding number of bytes,
     *         null if the input is null or empty
     */
    public static Long parseDataSize(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "(-?[0-9]+\\.?[0-9]*)\\s*(\\S*)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        double numeral = 0.0;
        Unit unit = null;
        if (matcher.matches()) {
            numeral = Double.parseDouble(matcher.group(1));
            String unitName = matcher.group(2);
            if (StringUtils.isEmpty(unitName)) {
                unit = Unit.bytes;
            } else {
                for (Unit unitEnum : Unit.values()) {
                    if (unitName.equalsIgnoreCase(unitEnum.name()) || StringUtils.startsWithIgnoreCase(unitEnum.name(), unitName)) {
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

        return unit.getValue(numeral);
    }

    /**
     * Convert a number into its ordinal form in English, e.g. 31 => "31st".
     *
     * From http://stackoverflow.com/q/6810336
     *
     * @param number Value to ordinalize.
     * @return Ordinal string form of number.
     */
    public static String ordinalize(int number) {
        // Array index matches up to digit in ones place
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };

        switch (number % 100) {
            case 11:
            case 12:
            case 13:
                return number + "th";
            default:
                return number + suffixes[number % 10];

        }
    }

    /**
     * Parse a string to an Integer, Long, BigInteger, or BigDecimal. Delegates to {@link NumberFormat#parse(String)}
     * except replaces the double type for floating point numbers with a {@link BigDecimal}.
     *
     * @param s string to parse as a number
     * @return an Integer, Long, BigInteger, or BigDecimal depending on the value of {@code s}
     * @throws NumberFormatException if s is not a number
     */
    public static Number createNumberUsingBigDecimal(final String s) {

        final Number number;

        try {

            final Number nfResult = NumberFormat.getInstance().parse(s);
            final String nfResultString = nfResult.toString();
            if (nfResultString.contains(".")) {
                // NumberFormat will make a number that uses a decimal point character as the floating point.
                // Unlike {@link #createNumber}, we never want to use float/double types.
                number = new BigDecimal(nfResultString);
            } else {

                number = createNumber(nfResultString);
            }

        } catch (final ParseException e) {
            throw new NumberFormatException("For input String: " + s);
        }

        return number;
    }

}
