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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApiFilter {
    /**
     * @return the name of the component property. This property's value will be the left
     * operand of {@link #operator}
     */
    @Nonnull
    String getProperty();

    /**
     * @return the predicate operator
     */
    @Nullable
    PredicateOperator getOperator();

    /**
     * @return the right operand of {@link #operator}
     */
    @Nullable
    String getValue();

    /**
     * @return true if the {@link #property} and {@link #operator} match the given values,
     * false otherwise
     */
    boolean matches(@Nonnull String property,
                    @Nonnull PredicateOperator operator);

    /**
     * Throws a validation error declaring this filter unsupported.
     */
    void unsupported();

    String asMatrixParam();
}
