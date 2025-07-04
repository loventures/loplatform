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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.learningobjects.cpxp.service.exception.ValidationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A predicate that defines components with certain properties. For example, the predicate
 * {@code name:eq(Alice)} defines components whose {@code name} property is equal to
 * "Alice".
 */
public class BaseApiFilter implements ApiFilter {

    /**
     * the name of the component property. This property's value will be the left operand
     * of {@link #operator}
     */
    private final String property;

    /**
     * the predicate operator
     */
    private final PredicateOperator operator;

    /**
     * the right operand of {@link #operator}
     */
    private final String value;

    @JsonCreator
    public BaseApiFilter(@JsonProperty("property") @Nonnull final String property,
                         @JsonProperty("operator") @Nullable final PredicateOperator operator,
                         @JsonProperty("value") @Nullable final String value) {
        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    /**
     * @return the name of the component property. This property's value will be the left
     * operand of {@link #operator}
     */
    @Override
    @Nonnull
    public String getProperty() {
        return property;
    }

    /**
     * @return the predicate operator
     */
    @Override
    @Nullable
    public PredicateOperator getOperator() {
        return operator;
    }

    /**
     * @return the right operand of {@link #operator}
     */
    @Override
    @Nullable
    public String getValue() {
        return value;
    }

    /**
     * @return true if the {@link #property} and {@link #operator} match the given values,
     * false otherwise
     */
    @Override
    public boolean matches(@Nonnull String property,
                           @Nonnull PredicateOperator operator) {
        return this.property.equals(property) && (this.operator == operator);
    }

    /**
     * Throws a validation error declaring this filter unsupported.
     */
    @Override
    public void unsupported() throws ValidationException {
        throw new ValidationException("filter", toString(), "Unsupported filter");
    }

    @Override
    public String toString() {
        return asMatrixParam();
    }

    @Override
    public String asMatrixParam() {
        String op = (operator != null) ? ":" + operator : "";
        return property + op + "(" + value + ")";
    }

    public static Iterable<String> asMatrixParams(Iterable<ApiFilter> filters) {
        return Iterables.transform(filters, new Function<ApiFilter, String>() {
            @Nullable
            @Override
            public String apply(@Nullable ApiFilter apiFilter) {
                return apiFilter.asMatrixParam();
            }
        });
    }
}
