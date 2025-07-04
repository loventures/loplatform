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
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static final Joiner.MapJoiner MATRIX_PARAM_JOINER =
            Joiner.on(";").withKeyValueSeparator("=");

    public static final Splitter.MapSplitter MATRIX_PARAM_SPLITTER =
            Splitter.on(';').withKeyValueSeparator("=");

    public static final Splitter PATH_SPLITTER = Splitter.on('/');

    private static final Map<Character, String> REPLACE1 = new HashMap<>();
    private static final Map<Character, String> REPLACE2 = new HashMap<>();
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w++\\b");

    static {
        // pre-normalization
        REPLACE1.put('\u00e4', "ae"); // a umlaut
        REPLACE1.put('\u00f1', "ny"); // n squigle
        REPLACE1.put('\u00f6', "oe"); // o umlaut
        REPLACE1.put('\u00fc', "ue"); // u umlaut
        REPLACE1.put('\u00ff', "yu"); // y umlaut

        // post-normalization
        REPLACE2.put('\u00df', "ss"); // German sharp s  -> ss
        REPLACE2.put('\u00c6', "AE"); // AE
        REPLACE2.put('\u00e6', "ae"); // ae
        REPLACE2.put('\u0132', "IJ"); // IJ
        REPLACE2.put('\u0133', "ij"); // ij
        REPLACE2.put('\u0152', "Oe"); // OE
        REPLACE2.put('\u0153', "oe"); // oe
        REPLACE2.put('\u00d0', "D");
        REPLACE2.put('\u0110', "D");
        REPLACE2.put('\u00f0', "d");
        REPLACE2.put('\u0111', "d");
        REPLACE2.put('\u0126', "H");
        REPLACE2.put('\u0127', "h");
        REPLACE2.put('\u0131', "i");
        REPLACE2.put('\u0138', "k");
        REPLACE2.put('\u013f', "L");
        REPLACE2.put('\u0141', "L");
        REPLACE2.put('\u0140', "l");
        REPLACE2.put('\u0142', "l");
        REPLACE2.put('\u014a', "N");
        REPLACE2.put('\u0149', "n");
        REPLACE2.put('\u014b', "n");
        REPLACE2.put('\u00d8', "O");
        REPLACE2.put('\u00f8', "o");
        REPLACE2.put('\u017f', "s");
        REPLACE2.put('\u00de', "T");
        REPLACE2.put('\u0166', "T");
        REPLACE2.put('\u00fe', "t");
        REPLACE2.put('\u0167', "t");
    }

    /**
     * A {@link Function} that removes underscores from a string.
     */
    public static final Function<String, String> REMOVE_UNDERSCORES = new Function<String, String>() {

        @Nullable
        @Override
        public String apply(@Nullable final String string) {
            return StringUtils.replace(string, "_", "");
        }
    };

    /**
     * Asciifies a string, stripping accents and converting compound characters
     * to latin letters.
     */
    public static String asciify(String string) {
        string = subst(string, REPLACE1);
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        string = subst(string, REPLACE2);
        string = string.replaceAll("[^\\p{ASCII}]", "");

        return string;
    }

    private static String subst(String string, Map<Character, String> map) {
        StringBuilder result = new StringBuilder();
        for (char c : string.toCharArray()) {
            String s = map.get(c);
            if (s == null) {
                result.append(c);
            } else {
                result.append(s);
            }
        }
        return result.toString();
    }

    public static String cleanFilename(String name) {
        name = StringUtils.asciify(name); // remove accents
        name = name.replaceAll("[^-a-zA-Z0-9.]+", "_"); // strip down most
        // special chars
        return name;
    }

    /**
     * Appends {@code value} to the {@code filename} before the filename's extension
     * (if present)
     */
    public static String appendFilename(final String filename, String value) {

        final String name = StringUtils.substringBeforeLast(filename, ".");
        final String extension = StringUtils.substringAfterLast(filename, ".");

        final String appended = name + value;

        if (isNotBlank(extension)) {
            return appended + "." + extension;
        } else {
            return appended;
        }
    }

    private static final Pattern SPLIT_RE = Pattern.compile("[,\\s]+");

    /**
     * Splits on comma and whitespace, returns empty list for no results.
     */
    public static String[] splitString(String str) {
        if (isEmpty(str) || SPLIT_RE.matcher(str).matches()) {
            return new String[0];
        } else {
            String[] s = SPLIT_RE.split(str);
            int n = s.length;
            if (isEmpty(s[0])) { // leading separators
                String[] t = new String[--n];
                System.arraycopy(s, 1, t, 0, n);
                s = t;
            }
            if (isEmpty(s[n - 1])) { // trailing separators
                String[] t = new String[--n];
                System.arraycopy(s, 0, t, 0, n);
                s = t;
            }
            return s;
        }
    }

    private static final Pattern CAMEL_CASE_PATTERN = Pattern
            .compile("([a-z](?=[A-Z])|[A-Z](?=[A-Z][a-z])|[A-Z](?=[A-Z]))");

    /**
     * Converts camel-cased strings (<em>e.g.</em>, fooBar, FooBar) to
     * underscored strings (<em>e.g.</em>, foo_Bar, Foo_Bar), disturbing the
     * original value as little as possible (no extraneous underscores are
     * added), and does not alter the case unless explicitly requested.
     *
     * The following table illustrates typical conversions (without
     * upper-casing).
     *
     * <table>
     * <tr>
     * <td>a</td>
     * <td>a</td>
     * </tr>
     * <tr>
     * <td>aaa</td>
     * <td>aaa</td>
     * </tr>
     * <tr>
     * <td>fooBar</td>foo_bar
     * <td></td>
     * </tr>
     * <tr>
     * <td>FooBar</td>
     * <td>Foo_Bar</td>
     * </tr>
     * <tr>
     * <td>Foo_BarBaz</td>
     * <td>Foo_Bar_Baz</td>
     * </tr>
     * <tr>
     * <td>fooBar__Baz</td>
     * <td>foo_Bar__Baz</td>
     * </tr>
     * <tr>
     * <td>aBC</td>
     * <td>a_B_C</td>
     * </tr>
     * <tr>
     * <td>ABC</td>
     * <td>A_B_C</td>
     * </tr>
     * </table>
     *
     * @param camelCased
     *            Original camel-cased string to convert.
     * @param upperCased
     *            Flag indicating whether the value should be converted to upper
     *            case.
     *
     * @return Underscored and (optionally) upper-cased form.
     *
     * @since 1.7.2
     *
     * @see String#toUpperCase()
     */
    public static String toUnderscored(final String camelCased, final boolean upperCased) {
        if (null == camelCased) {
            return null;
        }
        final String underscored = CAMEL_CASE_PATTERN.matcher(camelCased).replaceAll("$1_");
        return upperCased ? underscored.toUpperCase() : underscored;
    }

    public static String toUnderscored(final String camelCased) {
        return toUnderscored(camelCased, true);
    }

    public static String toLowerCaseUnderscored(final String camelCased) {
        return toUnderscored(camelCased, false).toLowerCase();
    }

    public static String toSeparateWords(String str) { // FooBar -> Foo Bar
        return CAMEL_CASE_PATTERN.matcher(str).replaceAll("$1 ");
    }

    private static final Pattern LOWER_CASE_PATTERN = Pattern.compile("^[a-z]+");

    /**
     * Returns the camel case prefix of a string. Soo fooBarBaz will return foo.
     */
    public static String camelCasePrefix(String string) {
        Matcher matcher = LOWER_CASE_PATTERN.matcher(string);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Returns the camel case suffix of a string. Soo fooBarBaz will return BarBaz.
     */
    public static String camelCaseSuffix(String string) {
        Matcher matcher = LOWER_CASE_PATTERN.matcher(string);
        return matcher.find() ? string.substring(matcher.end()) : null;
    }

    /**
     * Returns the string with the first character in lower case.
     */
    public static String toLowerCaseFirst(String str) {
        return StringUtils.isEmpty(str) ? str : str.substring(0, 1).toLowerCase().concat(str.substring(1));
    }

    /**
     * Returns the string with the first character in upper case.
     */
    public static String toUpperCaseFirst(final String str) {
        return StringUtils.isEmpty(str) ? str : str.substring(0, 1).toUpperCase().concat(str.substring(1));
    }

    public static String truncate255AndAppendEllipses(String sb) {
        if (sb.length() > 255) {
            return sb.substring(0,251)+"...";
        }
        return sb;
    }

    public static String escapeSqlLike(String s) {
        s = StringUtils.replace(s, "\\", "\\\\");
        s = StringUtils.replace(s, "%", "\\%");
        s = StringUtils.replace(s, "_", "\\_");
        return s;
    }


    // This is a crude CSV implementation; it does not handle
    // embedded newlines and may not be totally robust.
    // http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm
    public static List<String> splitCsv(String line, String separator) {
        List<String> tokens = new ArrayList<String>();
        String sep = Pattern.quote(separator);
        Pattern pattern = Pattern.compile("(?:^|" + sep + ")\\s*(\"(?:[^\"]|\"\")*\"|[^" + sep + "]*)");
        Matcher matcher = pattern.matcher(" " + line); // Without leading space ",a,a" results in [ "", "a" ]
        while (matcher.find()) {
            tokens.add(matcher.group()
                    .replaceAll("^" + sep, "") // remove first separator if any
                    .trim() // trim whitespace
                    .replaceAll("^?\"(.*)\"$", "$1") // remove outer quotations if any
                    .replaceAll("\"\"", "\"") // replace double inner quotations if any
                    .trim()); // trim again, because we don't want whitespace inside quotes
        }
        return tokens;
    }

    /**
     * Returns true if the given string is a quoted string, that is, it starts and ends with double quotes or single qoutes.
     *
     * The apache version of this method returns true for a single single quote or a single double quote character, which IMO is incorrect.
     *
     * <pre>
     *     isQuotedString("\"\"")             true
     *     isQuotedString("''")               true
     *     isQuotedString("\"foo\"")          true
     *     isQuotedString("hello, \"world\"") false
     *     isQuotedString(null)               false
     *     isQuotedString("\"")               false
     * </pre>
     *
     * @param s the string to check
     * @return true if the given string is a quoted string, false otherwise.
     */
    public static boolean isQuotedString(@Nullable final String s) {
        return s != null && s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));

    }

    /**
     * Given a series of delimited strings containing longs, extract the longs
     * e.g. ["1,2", "3,4", "5"] => [1,2,3,4,5]
     * @param lists
     * @param delimiter
     * @return
     */
    public static Set<Long> splitDelimitedLongs(Iterable<String> lists, String delimiter) {
        return splitDelimitedLongs(Joiner.on(delimiter).skipNulls().join(lists), delimiter);
    }

    /**
     * Given a delimited string containing longs, extract the longs
     * e.g. "1,2,3,4,5" => [1,2,3,4,5]
     *
     * @param delimitedString
     * @param delimiter
     * @return
     */
    public static Set<Long> splitDelimitedLongs(String delimitedString, String delimiter) {
        if (delimitedString == null) return new HashSet<>();
        final Iterable<String> strings = Splitter.on(delimiter).omitEmptyStrings().trimResults().split(delimitedString);
        return StreamSupport.stream(strings.spliterator(), false).map(stringToLong()).collect(Collectors.toSet());
    }

    public static Set<Long> splitDelimitedLongs(Iterable<String> strings) {
        return splitDelimitedLongs(strings, ",");
    }

    public static String[] tokenizeToStringArray(final String str, final String delimiter) {

        final Iterable<String> parts =
          Splitter.on(delimiter).trimResults().omitEmptyStrings().split(str);

        return Iterables.toArray(parts, String.class);
    }

    //TODO replace this with Longs.stringConverter() once we are on Guava 18+ (cf. JIRA ticket PLAT-282)
    public static Function<String, Long> stringToLong() {
        return new Function<String, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable String s) {
                return Long.parseLong(s);
            }
        };
    }

    // Sorts, removes common prefixes up to '.', joins with ", ".
    public static String niceList(Iterable<String> names) {
        String[] array = Iterables.toArray(names, String.class);
        if (array.length == 0) {
            return "";
        }
        Arrays.sort(array);
        int prefixLen = 1 + array[0].lastIndexOf('.',
          StringUtils.getCommonPrefix(array).length() - 1);
        if (prefixLen > 0) {
            for (int i = 0; i < array.length; ++i) {
                array[i] = array[i].substring(prefixLen);
            }
        }
        return StringUtils.join(array, ", ");
    }
}
