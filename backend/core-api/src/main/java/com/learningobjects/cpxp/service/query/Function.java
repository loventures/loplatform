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

public enum Function {
    NONE(false), // NONE is just used when an annotation requires a default value
    COUNT(true),
    COUNT_DISTINCT(true),
    SUM(false),
    AVG(false),
    MIN(false),
    MAX(false),
    LOWER(false),
    RANDOM(false),
    ID(false),
    LAST_MODIFIED(false),
    AVG_WITH_COUNT(false); // technically is partially an aggregate but only used on finderized data

    private boolean _aggregate;

    Function(boolean aggregate) {
        _aggregate = aggregate;
    }

    public boolean isAggregate() {
        return _aggregate;
    }

    public boolean coalesceToZeroInOrder() {
        return (this == AVG) || (this == AVG_WITH_COUNT) || (this == MIN) || (this == MAX) || (this == SUM);
    }

    public String toSQL(String field) {
        if (this == COUNT_DISTINCT) {
            return "COUNT(DISTINCT(" + field + "))";
        } else if (this == AVG_WITH_COUNT) {
            return "AVG(" + field + "), COUNT(" + field + ")";
        } else {
            return name() + "(" + field + ")";
        }
    }
}
