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
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.query.Direction;

import javax.annotation.Nonnull;

/**
 * An ordering to apply to a component property.
 */
public class BaseApiOrder implements ApiOrder {

    /**
     * The name of the component property.
     */
    private final String property;

    /**
     * Order direction.
     */
    private final OrderDirection direction;

    @JsonCreator
    public BaseApiOrder(@JsonProperty("property") @Nonnull final String property,
                        @JsonProperty("direction") @Nonnull final OrderDirection direction) {
        this.property = property;
        this.direction = direction;
    }

    @Override
    @Nonnull
    public String getProperty() {
        return property;
    }

    @Override
    @Nonnull
    public OrderDirection getDirection() {
        return direction;
    }

    @Override
    @Nonnull
    public Direction getQbDirection() {
        return direction.getDirection();
    }

    @Override
    public void unsupported() throws ValidationException {
        throw new ValidationException("order", toString(), "Unsupported order");
    }

    @Override
    public String toString() {
        return property + ":" + direction;
    }
}
