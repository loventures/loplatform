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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.learningobjects.cpxp.util.StringUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Exhaustive list of logical operators to apply to <b>all</b> the {@link
 * BaseApiFilter}s of a filter. Finer grained expression is not
 * supported at this time.
 */
public enum FilterOperator {

    AND("and"),
    OR("or"),

    /**
     * @deprecated use {@link #AND}
     */
    @Deprecated ALL("all"),

    /**
     * @deprecated use {@link #OR}
     */
    @Deprecated ANY("any"),;

    private static final Set<FilterOperator> CONJUNCTION_OPERATORS = EnumSet.of(AND, ALL);
    private static final Set<FilterOperator> DISJUNCTION_OPERATORS = EnumSet.of(OR, ANY);

    private static final Function<FilterOperator, String> GET_NAME =
            new Function<FilterOperator, String>() {
                @Override
                public String apply(final FilterOperator fc) {
                    return fc.name;
                }
            };

    private static final Map<String, FilterOperator> NAME_INDEX =
            Maps.uniqueIndex(Arrays.asList(FilterOperator.values()), GET_NAME);

    private final String name;

    private FilterOperator(final String name) {
        this.name = name;
    }

    public boolean isConjunction() {
        return CONJUNCTION_OPERATORS.contains(this);
    }

    public boolean isDisjunction() {
        return DISJUNCTION_OPERATORS.contains(this);
    }

    @Nullable
    public static FilterOperator byName(@Nullable final String name) {
        return NAME_INDEX.get(StringUtils.lowerCase(name));
    }

    @Override
    public String toString() {
        return name;
    }

}
