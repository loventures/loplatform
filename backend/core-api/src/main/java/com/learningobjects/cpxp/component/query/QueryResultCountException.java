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

/**
 * This exception occurs when an ApiQueryResults instance is attempted to be converted to {@link java.util.Optional}
 * Usually happens when a programmer assumes that a query returns 0 or 1, but it actually returns more than 1.
 */
public class QueryResultCountException extends RuntimeException {

    private QueryResultCountException(String message) {
        super(message);
    }

    public static QueryResultCountException forResultCount(final Integer resultCount) {
        final String msg = "Expected no more than 1 result but found: " + resultCount + ".";
        return new QueryResultCountException(msg);
    }
}
