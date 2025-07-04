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

// AGGREGATE_ITEM_WITH_DATA is a hack. Projection needs to be
// a first-class citizen..
public enum Projection {
    ID,
    ITEM,
    PARENT_ID,
    PARENT,
    ROOT_ID,
    ROOT,
    DATA,
    AGGREGATE_ITEM_WITH_DATA,
    ITEM_WITH_AGGREGATE,
    ITEM_CONTEXT,
    CALENDAR_INFO;

    public boolean multiple() {
        return (this == AGGREGATE_ITEM_WITH_DATA) || (this == ITEM_WITH_AGGREGATE);
    }

    public boolean nativeQuery() {
        return (this == ITEM_CONTEXT) || (this == CALENDAR_INFO);
    }
}
