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
import com.learningobjects.cpxp.service.query.Direction;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum OrderDirection {

    ASC("asc", Direction.ASC),
    DESC("desc", Direction.DESC),
    DESC_NULLS_LAST("descNullsLast", Direction.DESC_NULLS_LAST),
    ASC_NULLS_FIRST("ascNullsFirst", Direction.ASC_NULLS_FIRST);

    private final String name;

    private final Direction direction;

    OrderDirection(final String name, final Direction direction) {
        this.name = name;
        this.direction = direction;
    }

    private static final Map<String, OrderDirection> NAME_INDEX;

    static {
        final Map<String, OrderDirection> nameIndex = new HashMap<>();
        for (OrderDirection operator : OrderDirection.values()) {
            nameIndex.put(operator.getName().toLowerCase(), operator);
        }
        NAME_INDEX = Collections.unmodifiableMap(nameIndex);
    }

    public String getName() {
        return name;
    }

    public Direction getDirection() {
        return direction;
    }

    @Nullable
    @JsonCreator
    public static OrderDirection byName(@Nullable final String name) {
        return NAME_INDEX.get(name == null ? null : name.toLowerCase());
    }

    @Override
    public String toString() {
        return name;
    }
}
