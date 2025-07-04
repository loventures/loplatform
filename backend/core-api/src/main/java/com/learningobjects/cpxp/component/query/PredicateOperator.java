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

package com.learningobjects.cpxp.component.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.learningobjects.cpxp.service.query.Comparison;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Exhaustive list of operators for use in {@link ApiFilter}
 */
public enum PredicateOperator {

    // less magical than the earlier enum, more declarative,
    // lets lower layers be dumber about defaults

    EQUALS("equals", "eq", Comparison.eq, SqlLiftStrategy.IDENTITY),

    NOT_EQUALS("notEquals", "ne", Comparison.ne, SqlLiftStrategy.IDENTITY),

    LESS_THAN("lessThan", "lt", Comparison.lt, SqlLiftStrategy.IDENTITY),

    LESS_THAN_OR_EQUALS("lessThanOrEquals", "le", Comparison.le,
      SqlLiftStrategy.IDENTITY),

    GREATER_THAN("greaterThan", "gt", Comparison.gt, SqlLiftStrategy.IDENTITY),

    GREATER_THAN_OR_EQUALS("greaterThanOrEquals", "ge", Comparison.ge,
      SqlLiftStrategy.IDENTITY),

    STARTS_WITH("startsWith", "sw", Comparison.like, SqlLiftStrategy.STARTS_WITH),

    ENDS_WITH("endsWith", "ew", Comparison.like, SqlLiftStrategy.ENDS_WITH),

    CONTAINS("contains", "co", Comparison.like, SqlLiftStrategy.CONTAINS),

    IN("in", "in", Comparison.in, SqlLiftStrategy.IDENTITY),

    NULL("isNull", "isNull", Comparison.eq, SqlLiftStrategy.NULL),

    NON_NULL("isNonNull", "isNonNull", Comparison.ne, SqlLiftStrategy.NULL),

    TEXT_SEARCH("textSearch", "ts", Comparison.search, SqlLiftStrategy.IDENTITY),

    INTERSECTS("intersects", "∩", Comparison.intersects, SqlLiftStrategy.ARRAY);

    private final String _longhand;
    private final String _shorthand;

    /**
     * the query builder comparison to use
     */
    private final Comparison _comparison;

    /**
     * the transformation to apply to the right hand operand to make it SQL safe and to
     * make it work right with {@link #_comparison}.
     */
    private final SqlLiftStrategy _sqlLiftStrategy;

    private PredicateOperator(final String longhand, final String shorthand,
                              final Comparison comparison, final SqlLiftStrategy sqlLiftStrategy) {
        _longhand = longhand;
        _shorthand = shorthand;
        _comparison = comparison;
        _sqlLiftStrategy = sqlLiftStrategy;
    }

    private static final Map<String, PredicateOperator> NAME_INDEX;

    static {
        final Map<String, PredicateOperator> nameIndex = new HashMap<>();
        for (PredicateOperator operator : PredicateOperator.values()) {
            nameIndex.put(operator._longhand.toLowerCase(), operator);
            nameIndex.put(operator._shorthand.toLowerCase(), operator);
        }
        NAME_INDEX = Collections.unmodifiableMap(nameIndex);
    }

    @Nullable
    @JsonCreator
    public static PredicateOperator byName(@Nullable final String name) {
        return NAME_INDEX.get(name == null ? null : name.toLowerCase());
    }

    public Comparison getComparison() {
        return _comparison;
    }

    public SqlLiftStrategy getSqlLiftStrategy() {
        return _sqlLiftStrategy;
    }

    @Override
    public String toString() {
        return _shorthand;
    }

    public static enum SqlLiftStrategy {

        IDENTITY {
            @Override
            public String apply(final String value) {
                return value;
            }
        },
        STARTS_WITH {
            @Override
            public String apply(final String value) {
                return escapeSqlLike(value) + "%";
            }
        },
        ENDS_WITH {
            @Override
            public String apply(final String value) {
                return "%" + escapeSqlLike(value);
            }
        },
        CONTAINS {
            @Override
            public String apply(final String value) {
                return "%" + escapeSqlLike(value) + "%";
            }
        },
        ARRAY {
            @Override
            public String apply(final String value) {
                return "{" + value + "}";
            }
        },
        NULL {
            @Override
            public String apply(final String value) {
                return null;
            }
        };

        public abstract String apply(final String value);
    }

    private static String escapeSqlLike(String s) {
        s = replace(s, "\\", "\\\\");
        s = replace(s, "%", "\\%");
        s = replace(s, "_", "\\_");
        return s;
    }

    /**
     * <p>Replaces all occurrences of a String within another String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * <pre>
     * replace(null, *, *)        = null
     * replace("", *, *)          = ""
     * replace("any", null, *)    = "any"
     * replace("any", *, null)    = "any"
     * replace("any", "", *)      = "any"
     * replace("aba", "a", null)  = "aba"
     * replace("aba", "a", "")    = "b"
     * replace("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @param text         text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement  the String to replace it with, may be null
     * @return the text with any replacements processed,
     * <code>null</code> if null String input
     * @see #replace(String text, String searchString, String replacement, int max)
     */
    private static String replace(String text, String searchString, String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    /**
     * <p>Replaces a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * <pre>
     * replace(null, *, *, *)         = null
     * replace("", *, *, *)           = ""
     * replace("any", null, *, *)     = "any"
     * replace("any", *, null, *)     = "any"
     * replace("any", "", *, *)       = "any"
     * replace("any", *, *, 0)        = "any"
     * replace("abaa", "a", null, -1) = "abaa"
     * replace("abaa", "a", "", -1)   = "b"
     * replace("abaa", "a", "z", 0)   = "abaa"
     * replace("abaa", "a", "z", 1)   = "zbaa"
     * replace("abaa", "a", "z", 2)   = "zbza"
     * replace("abaa", "a", "z", -1)  = "zbzz"
     * </pre>
     *
     * @param text         text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement  the String to replace it with, may be null
     * @param max          maximum number of values to replace, or <code>-1</code> if no maximum
     * @return the text with any replacements processed,
     * <code>null</code> if null String input
     */
    private static String replace(String text, String searchString, String replacement, int max) {
        if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end == INDEX_NOT_FOUND) {
            return text;
        }
        int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = (increase < 0 ? 0 : increase);
        increase *= (max < 0 ? 16 : (max > 64 ? 64 : max));
        StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end != INDEX_NOT_FOUND) {
            buf.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    /**
     * <p>Checks if a String is empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     *
     * <p>NOTE: This method changed in Lang version 2.0.
     * It no longer trims the String.
     * That functionality is available in isBlank().</p>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * Represents a failed index search.
     *
     * @since 2.1
     */
    private static final int INDEX_NOT_FOUND = -1;
}

