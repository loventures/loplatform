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

/**
 * Utility class providing syntactical support for QL statement construction in
 * the form of conditional appends. Also contains some common QL symbols.
 */
class QuerySyntax {
    private static final QuerySyntax SINGLETON = new QuerySyntax();

    static final String AND = " AND ";

    static final String OR = " OR ";

    static final String L_PAREN = "(";

    static final String R_PAREN = ")";

    private QuerySyntax() {
        // enforce Singleton pattern
    }

    /**
     * Append an AND onto the buffer but only if the buffer is empty.
     *
     * @param buffer
     *            to modify
     */
    static StringBuilder and(StringBuilder buffer) {
        return SINGLETON.conditionalAppend(buffer.length() > 0, buffer, AND);
    }

    /**
     * Append an OR onto the buffer but only if the buffer is empty.
     *
     * @param buffer
     *            to modify
     */
    static StringBuilder or(StringBuilder buffer) {
        return SINGLETON.conditionalAppend(buffer.length() > 0, buffer, OR);
    }

    /**
     * Conditional append.
     *
     * @param test
     *            only append if this is true
     * @param buffer
     *            to modify
     * @param junction
     *            token or fragment to append
     */
    static StringBuilder append(boolean test, StringBuilder buffer, String junction) {
        return SINGLETON.conditionalAppend(test, buffer, junction);
    }

    private StringBuilder conditionalAppend(boolean test, StringBuilder sqlBuffer,
            String token) {
        if (!test) {
            return sqlBuffer;
        }

        return sqlBuffer.append(token);
    }

}
